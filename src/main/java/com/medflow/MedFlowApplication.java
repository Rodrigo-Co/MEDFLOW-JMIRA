package com.medflow;

import com.fasterxml.jackson.core.type.TypeReference;
import com.medflow.config.AppConfig;
import com.medflow.dto.ChangePasswordRequest;
import com.medflow.dto.CreateChangeRequest;
import com.medflow.dto.CreateDoctorRequest;
import com.medflow.dto.CreatePatientRequest;
import com.medflow.dto.CreateRecordRequest;
import com.medflow.dto.CreateTicketRequest;
import com.medflow.dto.DashboardStats;
import com.medflow.dto.DoctorStat;
import com.medflow.dto.EditRecordRequest;
import com.medflow.dto.FormatNotesRequest;
import com.medflow.dto.PatientLoginRequest;
import com.medflow.dto.ResolveChangeRequest;
import com.medflow.dto.StaffLoginRequest;
import com.medflow.dto.UpdateDoctorRequest;
import com.medflow.dto.Verify2FARequest;
import com.medflow.http.AuthenticatedUser;
import com.medflow.http.HttpException;
import com.medflow.http.RequestContext;
import com.medflow.model.User;
import com.medflow.persistence.Database;
import com.medflow.repository.AuditLogRepository;
import com.medflow.repository.DataChangeRequestRepository;
import com.medflow.repository.MedicalRecordRepository;
import com.medflow.repository.TicketRepository;
import com.medflow.repository.UserRepository;
import com.medflow.security.JwtUtil;
import com.medflow.service.AuthService;
import com.medflow.service.ChangeRequestService;
import com.medflow.service.RecordService;
import com.medflow.service.TicketService;
import com.medflow.service.TwoFactorService;
import com.medflow.service.UserService;
import com.medflow.util.Jsons;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MedFlowApplication {

    public static void main(String[] args) throws Exception {
        AppConfig config = new AppConfig();
        Database database = new Database(config);

        UserRepository userRepo = new UserRepository(database);
        TicketRepository ticketRepo = new TicketRepository(database);
        MedicalRecordRepository medicalRecordRepo = new MedicalRecordRepository(database);
        DataChangeRequestRepository changeRequestRepo = new DataChangeRequestRepository(database);
        AuditLogRepository auditLogRepo = new AuditLogRepository(database);

        JwtUtil jwtUtil = new JwtUtil(config);
        var auditService = new com.medflow.service.AuditService(auditLogRepo);
        TwoFactorService twoFactorService = new TwoFactorService(config);
        UserService userService = new UserService(userRepo, auditService);
        AuthService authService = new AuthService(userRepo, jwtUtil, auditService, twoFactorService);
        ChangeRequestService changeRequestService = new ChangeRequestService(changeRequestRepo, userRepo, auditService);
        TicketService ticketService = new TicketService(ticketRepo, userRepo, auditService);
        RecordService recordService = new RecordService(medicalRecordRepo, userRepo, auditService);

        int port = config.getInt("server.port", 8080);
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newFixedThreadPool(Math.max(4, Runtime.getRuntime().availableProcessors() * 2)));
        server.createContext("/", new AppHandler(
                config, jwtUtil, userRepo, ticketRepo,
                authService, userService, changeRequestService, ticketService, recordService, auditLogRepo
        ));
        server.start();

        System.out.println("MedFlow backend (Java puro) iniciado na porta " + port);
    }

    private static final class AppHandler implements HttpHandler {

        private final List<Route> routes = new ArrayList<>();
        private final List<String> allowedOrigins;
        private final JwtUtil jwtUtil;
        private final UserRepository userRepo;
        private final TicketRepository ticketRepo;
        private final AuthService authService;
        private final UserService userService;
        private final ChangeRequestService changeRequestService;
        private final TicketService ticketService;
        private final RecordService recordService;
        private final AuditLogRepository auditLogRepo;

        private AppHandler(AppConfig config,
                           JwtUtil jwtUtil,
                           UserRepository userRepo,
                           TicketRepository ticketRepo,
                           AuthService authService,
                           UserService userService,
                           ChangeRequestService changeRequestService,
                           TicketService ticketService,
                           RecordService recordService,
                           AuditLogRepository auditLogRepo) {
            this.allowedOrigins = config.getCsv("cors.allowed-origins");
            this.jwtUtil = jwtUtil;
            this.userRepo = userRepo;
            this.ticketRepo = ticketRepo;
            this.authService = authService;
            this.userService = userService;
            this.changeRequestService = changeRequestService;
            this.ticketService = ticketService;
            this.recordService = recordService;
            this.auditLogRepo = auditLogRepo;
            registerRoutes();
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                applyCors(exchange);
                if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                    sendEmpty(exchange, 204);
                    return;
                }

                String path = normalizePath(exchange.getRequestURI().getPath());
                if (!path.startsWith("/api")) {
                    serveStatic(exchange, path);
                    return;
                }

                AuthenticatedUser user = authenticate(exchange);
                for (Route route : routes) {
                    RouteMatch match = route.match(exchange.getRequestMethod(), path);
                    if (match == null) {
                        continue;
                    }
                    if (route.requiresAuth() && user == null) {
                        throw new HttpException(401, "Nao autenticado");
                    }
                    if (!route.allowedRoles().isEmpty()) {
                        if (user == null || route.allowedRoles().stream().noneMatch(role -> role.equals(user.role()))) {
                            throw new HttpException(403, "Acesso negado");
                        }
                    }
                    RequestContext.setCurrentUser(user);
                    try {
                        Object response = route.handler().handle(new RequestData(exchange, match.pathParams(), parseQuery(exchange.getRequestURI())));
                        sendJson(exchange, route.successStatus(), response);
                        return;
                    } finally {
                        RequestContext.clear();
                    }
                }

                throw new HttpException(404, "Rota nao encontrada");
            } catch (HttpException e) {
                RequestContext.clear();
                sendError(exchange, e.getStatus(), e.getMessage());
            } catch (Exception e) {
                RequestContext.clear();
                e.printStackTrace();
                sendError(exchange, 500, "Erro interno do servidor");
            } finally {
                exchange.close();
            }
        }

        private void registerRoutes() {
            routes.add(new Route("POST", "/api/auth/patient/login", List.of(), 200, req -> {
                PatientLoginRequest body = readBody(req, PatientLoginRequest.class);
                return authService.initPatientLogin(body.getEmail(), body.getPassword());
            }));
            routes.add(new Route("POST", "/api/auth/patient/register", List.of(), 201, req -> AuthService.toDto(userService.createPatientSelf(readBody(req, CreatePatientRequest.class)))));
            routes.add(new Route("POST", "/api/auth/doctor/login", List.of(), 200, req -> {
                StaffLoginRequest body = readBody(req, StaffLoginRequest.class);
                return authService.initDoctorLogin(body.getUsername(), body.getPassword());
            }));
            routes.add(new Route("POST", "/api/auth/admin/login", List.of(), 200, req -> {
                StaffLoginRequest body = readBody(req, StaffLoginRequest.class);
                return authService.loginAdmin(body.getUsername(), body.getPassword());
            }));
            routes.add(new Route("POST", "/api/auth/verify-2fa", List.of(), 200, req -> {
                Verify2FARequest body = readBody(req, Verify2FARequest.class);
                return authService.verify2FA(body.getSessionToken(), body.getCode());
            }));
            routes.add(new Route("POST", "/api/auth/resend-2fa", List.of(), 200, req -> {
                Map<String, String> body = readMap(req);
                String email = body.get("email");
                String username = body.get("username");
                String password = body.get("password");
                var response = email != null
                        ? authService.initPatientLogin(email, password)
                        : authService.initDoctorLogin(username, password);
                return Map.of("sessionToken", response.getSessionToken(), "maskedEmail", response.getMaskedEmail());
            }));
            routes.add(new Route("GET", "/api/auth/me", List.of("patient", "doctor", "admin"), 200, req -> {
                User user = userRepo.findById(RequestContext.currentUserId()).orElseThrow(() -> new HttpException(401, "Usuario nao encontrado"));
                return AuthService.toDto(user);
            }));
            routes.add(new Route("GET", "/api/auth/patients", List.of(), 200, req -> userRepo.findByRole("patient").stream().map(AuthService::toDto).toList()));
            routes.add(new Route("GET", "/api/auth/staff", List.of(), 200, req -> {
                String role = req.queryParam("role");
                if (!List.of("doctor", "admin").contains(role)) {
                    throw new HttpException(400, "Role invalida");
                }
                return userRepo.findByRole(role).stream().map(AuthService::toDto).toList();
            }));

            routes.add(new Route("GET", "/api/patients", List.of("doctor", "admin"), 200, req -> userService.allPatients().stream().map(AuthService::toDto).toList()));
            routes.add(new Route("GET", "/api/patients/{id}", List.of("patient", "doctor", "admin"), 200, req -> {
                String patientId = req.pathParam("id");
                if ("patient".equals(RequestContext.currentUserRole()) && !RequestContext.currentUserId().equals(patientId)) {
                    throw new HttpException(403, "Acesso negado");
                }
                return userService.getPatient(patientId);
            }));
            routes.add(new Route("POST", "/api/patients", List.of("doctor", "admin"), 201, req -> AuthService.toDto(userService.createPatient(readBody(req, CreatePatientRequest.class)))));
            routes.add(new Route("PUT", "/api/patients/{id}/profile", List.of("patient"), 200, req -> {
                String id = req.pathParam("id");
                if (!id.equals(RequestContext.currentUserId())) {
                    throw new HttpException(403, "Acesso negado");
                }
                Map<String, String> body = readMap(req);
                return AuthService.toDto(userService.updatePatientProfile(id, body.get("name"), body.get("email"), body.get("phone")));
            }));

            routes.add(new Route("GET", "/api/admin/doctors", List.of("admin"), 200, req -> userService.allDoctors().stream().map(AuthService::toDto).toList()));
            routes.add(new Route("POST", "/api/admin/doctors", List.of("admin"), 201, req -> AuthService.toDto(userService.createDoctor(readBody(req, CreateDoctorRequest.class)))));
            routes.add(new Route("PUT", "/api/admin/doctors/{id}", List.of("admin"), 200, req -> AuthService.toDto(userService.updateDoctor(req.pathParam("id"), readBody(req, UpdateDoctorRequest.class)))));

            routes.add(new Route("POST", "/api/users/change-password", List.of("patient", "doctor", "admin"), 200, req -> {
                userService.changePassword(RequestContext.currentUserId(), readBody(req, ChangePasswordRequest.class));
                return Map.of("message", "Senha alterada com sucesso");
            }));

            routes.add(new Route("GET", "/api/doctor/profile", List.of("doctor"), 200, req -> {
                User user = userRepo.findById(RequestContext.currentUserId()).orElseThrow(() -> new HttpException(404, "Medico nao encontrado"));
                return AuthService.toDto(user);
            }));
            routes.add(new Route("PUT", "/api/doctor/profile", List.of("doctor"), 200, req -> {
                User doctor = userRepo.findById(RequestContext.currentUserId()).orElseThrow(() -> new HttpException(401, "Usuario nao encontrado"));
                Map<String, String> body = readMap(req);
                String newEmail = body.get("email");
                if (newEmail != null && !newEmail.isBlank() && !newEmail.equals(doctor.getEmail())) {
                    CreateChangeRequest changeRequest = new CreateChangeRequest();
                    changeRequest.setFieldName("email");
                    changeRequest.setOldValue(doctor.getEmail());
                    changeRequest.setNewValue(newEmail);
                    changeRequestService.submit(changeRequest);
                }
                if (body.containsKey("name") && body.get("name") != null && !body.get("name").isBlank()) {
                    doctor.setName(body.get("name"));
                }
                if (body.containsKey("phone")) {
                    doctor.setPhone(body.get("phone"));
                }
                return AuthService.toDto(userRepo.save(doctor));
            }));
            routes.add(new Route("POST", "/api/doctor/change-request", List.of("doctor"), 201, req -> changeRequestService.submit(readBody(req, CreateChangeRequest.class))));

            routes.add(new Route("GET", "/api/admin/dashboard", List.of("admin"), 200, req -> {
                List<User> doctors = userService.allDoctors();
                DashboardStats stats = new DashboardStats();
                stats.setTotalDoctors(doctors.size());
                stats.setTicketsOpen(ticketRepo.countByStatus("aberto"));
                stats.setTicketsInProgress(ticketRepo.countByStatus("em_andamento"));
                stats.setTicketsConcluded(ticketRepo.countByStatus("concluido"));
                stats.setPendingRequests(changeRequestService.countPending());
                stats.setDoctors(doctors.stream().map(doctor -> {
                    DoctorStat stat = new DoctorStat();
                    stat.setId(doctor.getId());
                    stat.setName(doctor.getName());
                    stat.setSpecialty(doctor.getSpecialty());
                    stat.setCrm(doctor.getCrm());
                    stat.setAttendances(doctor.getAttendances() == null ? 0 : doctor.getAttendances());
                    stat.setSurgeries(doctor.getSurgeries() == null ? 0 : doctor.getSurgeries());
                    stat.setExams(doctor.getExams() == null ? 0 : doctor.getExams());
                    return stat;
                }).toList());
                return stats;
            }));
            routes.add(new Route("GET", "/api/admin/requests", List.of("admin"), 200, req -> {
                String status = req.queryParam("status");
                return "pendente".equals(status) ? changeRequestService.findPending() : changeRequestService.findAll();
            }));
            routes.add(new Route("PATCH", "/api/admin/requests/{id}/resolve", List.of("admin"), 200, req -> {
                ResolveChangeRequest body = readBody(req, ResolveChangeRequest.class);
                return changeRequestService.resolve(req.pathParam("id"), Boolean.TRUE.equals(body.getApproved()));
            }));
            routes.add(new Route("GET", "/api/admin/audit", List.of("admin"), 200, req -> {
                String severity = req.queryParam("severity");
                String role = req.queryParam("role");
                if (notBlank(severity) && notBlank(role)) {
                    return auditLogRepo.findBySeverityAndUserRoleOrderByLogTimestampDesc(severity, role);
                }
                if (notBlank(severity)) {
                    return auditLogRepo.findBySeverityOrderByLogTimestampDesc(severity);
                }
                if (notBlank(role)) {
                    return auditLogRepo.findByUserRoleOrderByLogTimestampDesc(role);
                }
                return auditLogRepo.findAllByOrderByLogTimestampDesc();
            }));

            routes.add(new Route("GET", "/api/tickets", List.of("doctor", "admin"), 200, req -> {
                String status = req.queryParam("status");
                return notBlank(status) ? ticketService.findByStatus(status) : ticketService.findAll();
            }));
            routes.add(new Route("GET", "/api/tickets/my", List.of("patient"), 200, req -> ticketService.findByPatient(RequestContext.currentUserId())));
            routes.add(new Route("GET", "/api/tickets/{id}", List.of("patient", "doctor", "admin"), 200, req -> ticketService.findById(req.pathParam("id"))));
            routes.add(new Route("POST", "/api/tickets", List.of("patient"), 201, req -> ticketService.create(readBody(req, CreateTicketRequest.class))));
            routes.add(new Route("PATCH", "/api/tickets/{id}/accept", List.of("doctor"), 200, req -> ticketService.accept(req.pathParam("id"))));
            routes.add(new Route("PATCH", "/api/tickets/{id}/complete", List.of("doctor"), 200, req -> ticketService.complete(req.pathParam("id"))));

            routes.add(new Route("GET", "/api/records", List.of("doctor", "admin"), 200, req -> recordService.findAll(req.queryParam("q"))));
            routes.add(new Route("GET", "/api/records/my", List.of("doctor"), 200, req -> recordService.findByDoctor(RequestContext.currentUserId())));
            routes.add(new Route("GET", "/api/records/patient/{patientId}", List.of("patient", "doctor", "admin"), 200, req -> {
                String patientId = req.pathParam("patientId");
                if ("patient".equals(RequestContext.currentUserRole()) && !RequestContext.currentUserId().equals(patientId)) {
                    throw new HttpException(403, "Acesso negado");
                }
                return recordService.findByPatient(patientId);
            }));
            routes.add(new Route("GET", "/api/records/{id}", List.of("patient", "doctor", "admin"), 200, req -> recordService.findById(req.pathParam("id"))));
            routes.add(new Route("POST", "/api/records", List.of("doctor"), 201, req -> recordService.create(readBody(req, CreateRecordRequest.class))));
            routes.add(new Route("PUT", "/api/records/{id}", List.of("doctor"), 200, req -> recordService.edit(req.pathParam("id"), readBody(req, EditRecordRequest.class))));
            routes.add(new Route("POST", "/api/records/format", List.of("doctor"), 200, req -> {
                FormatNotesRequest body = readBody(req, FormatNotesRequest.class);
                return Map.of("formattedNotes", recordService.formatNotes(body.getRawNotes(), body.getType()));
            }));
        }

        private <T> T readBody(RequestData req, Class<T> type) {
            try {
                return Jsons.MAPPER.readValue(req.body(), type);
            } catch (IOException e) {
                throw new HttpException(400, "JSON invalido");
            }
        }

        private Map<String, String> readMap(RequestData req) {
            try {
                return Jsons.MAPPER.readValue(req.body(), new TypeReference<>() {
                });
            } catch (IOException e) {
                throw new HttpException(400, "JSON invalido");
            }
        }

        private AuthenticatedUser authenticate(HttpExchange exchange) {
            String header = exchange.getRequestHeaders().getFirst("Authorization");
            if (header == null || !header.startsWith("Bearer ")) {
                return null;
            }
            return jwtUtil.parse(header.substring(7)).orElse(null);
        }

        private void applyCors(HttpExchange exchange) {
            String origin = exchange.getRequestHeaders().getFirst("Origin");
            Headers headers = exchange.getResponseHeaders();
            if (origin != null) {
                headers.set("Access-Control-Allow-Origin", origin);
                headers.set("Access-Control-Allow-Credentials", "true");
            }
            headers.set("Access-Control-Allow-Methods", "GET,POST,PUT,PATCH,DELETE,OPTIONS");
            headers.set("Access-Control-Allow-Headers", "Content-Type,Authorization");
        }

        private void serveStatic(HttpExchange exchange, String path) throws IOException {
            String resourcePath = "/".equals(path) ? "static/index.html" : "static" + path;
            if (resourcePath.endsWith("/")) {
                resourcePath += "index.html";
            }

            try (InputStream input = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
                if (input == null) {
                    sendError(exchange, 404, "Arquivo nao encontrado");
                    return;
                }
                byte[] bytes = input.readAllBytes();
                exchange.getResponseHeaders().set("Content-Type", contentType(path));
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream output = exchange.getResponseBody()) {
                    output.write(bytes);
                }
            }
        }

        private String contentType(String path) {
            if (path.endsWith(".css")) {
                return "text/css; charset=UTF-8";
            }
            if (path.endsWith(".js")) {
                return "application/javascript; charset=UTF-8";
            }
            if (path.endsWith(".html") || "/".equals(path)) {
                return "text/html; charset=UTF-8";
            }
            if (path.endsWith(".svg")) {
                return "image/svg+xml";
            }
            if (path.endsWith(".png")) {
                return "image/png";
            }
            if (path.endsWith(".jpg") || path.endsWith(".jpeg")) {
                return "image/jpeg";
            }
            return "text/plain; charset=UTF-8";
        }

        private void sendJson(HttpExchange exchange, int status, Object body) throws IOException {
            String json = Jsons.write(body);
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(bytes);
            }
        }

        private void sendError(HttpExchange exchange, int status, String message) throws IOException {
            sendJson(exchange, status, Map.of("message", message));
        }

        private void sendEmpty(HttpExchange exchange, int status) throws IOException {
            exchange.sendResponseHeaders(status, -1);
        }

        private Map<String, List<String>> parseQuery(URI uri) {
            Map<String, List<String>> values = new HashMap<>();
            String rawQuery = uri.getRawQuery();
            if (rawQuery == null || rawQuery.isBlank()) {
                return values;
            }
            for (String pair : rawQuery.split("&")) {
                String[] parts = pair.split("=", 2);
                String key = decode(parts[0]);
                String value = parts.length > 1 ? decode(parts[1]) : "";
                values.computeIfAbsent(key, ignored -> new ArrayList<>()).add(value);
            }
            return values;
        }

        private String normalizePath(String path) {
            return path == null || path.isBlank() ? "/" : path;
        }

        private String decode(String value) {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        }

        private boolean notBlank(String value) {
            return value != null && !value.isBlank();
        }
    }

    private record RequestData(HttpExchange exchange, Map<String, String> pathParams, Map<String, List<String>> queryParams) {
        String body() throws IOException {
            return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        }

        String pathParam(String key) {
            return pathParams.get(key);
        }

        String queryParam(String key) {
            List<String> values = queryParams.get(key);
            return values == null || values.isEmpty() ? null : values.get(0);
        }
    }

    private record Route(String method, String template, List<String> allowedRoles, int successStatus, RouteHandler handler) {
        RouteMatch match(String requestMethod, String path) {
            if (!method.equalsIgnoreCase(requestMethod)) {
                return null;
            }
            Pattern pattern = Pattern.compile("^" + template.replaceAll("\\{([^/]+)}", "(?<$1>[^/]+)") + "$");
            Matcher matcher = pattern.matcher(path);
            if (!matcher.matches()) {
                return null;
            }
            Map<String, String> params = new HashMap<>();
            Matcher nameMatcher = Pattern.compile("\\{([^/]+)}").matcher(template);
            while (nameMatcher.find()) {
                String name = nameMatcher.group(1);
                params.put(name, matcher.group(name));
            }
            return new RouteMatch(params);
        }

        boolean requiresAuth() {
            return !allowedRoles.isEmpty();
        }
    }

    private record RouteMatch(Map<String, String> pathParams) {
    }

    @FunctionalInterface
    private interface RouteHandler {
        Object handle(RequestData request) throws Exception;
    }
}
