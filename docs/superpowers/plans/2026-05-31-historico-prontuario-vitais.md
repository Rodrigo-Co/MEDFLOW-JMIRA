# Historico de Prontuario e Vitais Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add auditable medical-record ownership changes, detailed edit history, and doctor-driven patient vital/medication updates tied to a justificatory record.

**Architecture:** Keep the existing pure-Java HTTP server and JDBC repository style. Extend `RecordService` with explicit operations for record takeover and health updates, add focused DTO/model/repository methods, and wire the doctor UI through `app.js` and `doctor/records.html`.

**Tech Stack:** Java 17, Maven, JDBC PostgreSQL, static HTML/CSS/JavaScript, existing audit log model.

---

## File Structure

- Modify `src/main/resources/schema.sql`: add `patient_health_update_history` table for operational history.
- Create `src/main/java/com/medflow/dto/UpdatePatientHealthRequest.java`: request body for patient vital/medication updates.
- Create `src/main/java/com/medflow/model/PatientHealthUpdateHistory.java`: response model for health update history.
- Modify `src/main/java/com/medflow/repository/UserRepository.java`: add insert methods for blood pressure, heart rate, medication replacement, and health update history.
- Modify `src/main/java/com/medflow/repository/MedicalRecordRepository.java`: expose existing history persistence through `save` and keep detailed history loaded.
- Modify `src/main/java/com/medflow/service/RecordService.java`: add detailed edit summaries, `takeOver`, `updatePatientHealth`, and validation helpers.
- Modify `src/main/java/com/medflow/MedFlowApplication.java`: add routes `PATCH /api/records/{id}/take-over` and `POST /api/patients/{id}/health-updates`.
- Modify `src/main/resources/static/js/app.js`: add API client functions for takeover and health update.
- Modify `src/main/resources/static/pages/doctor/records.html`: show takeover action, edit history, and health update modal.
- Test with focused Java-level checks where possible and manual API/browser verification where local Java runtime is unavailable.

## Task 1: Database and DTO Surface

**Files:**
- Modify: `src/main/resources/schema.sql`
- Create: `src/main/java/com/medflow/dto/UpdatePatientHealthRequest.java`
- Create: `src/main/java/com/medflow/model/PatientHealthUpdateHistory.java`

- [ ] **Step 1: Add schema for health update history**

Add this table after `record_edit_history` in `schema.sql`:

```sql
CREATE TABLE IF NOT EXISTS patient_health_update_history (
    id              VARCHAR(50) PRIMARY KEY,
    patient_id      VARCHAR(50) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    patient_name    VARCHAR(255),
    record_id       VARCHAR(50) NOT NULL REFERENCES medical_records(id) ON DELETE CASCADE,
    doctor_id       VARCHAR(50) NOT NULL REFERENCES users(id),
    doctor_name     VARCHAR(255),
    update_timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    changes         TEXT NOT NULL
);
```

- [ ] **Step 2: Create request DTO**

Create `UpdatePatientHealthRequest.java`:

```java
package com.medflow.dto;

import java.util.List;

public class UpdatePatientHealthRequest {
    private String recordId;
    private Integer systolic;
    private Integer diastolic;
    private Integer heartRate;
    private List<String> medications;

    public String getRecordId() { return recordId; }
    public void setRecordId(String recordId) { this.recordId = recordId; }
    public Integer getSystolic() { return systolic; }
    public void setSystolic(Integer systolic) { this.systolic = systolic; }
    public Integer getDiastolic() { return diastolic; }
    public void setDiastolic(Integer diastolic) { this.diastolic = diastolic; }
    public Integer getHeartRate() { return heartRate; }
    public void setHeartRate(Integer heartRate) { this.heartRate = heartRate; }
    public List<String> getMedications() { return medications; }
    public void setMedications(List<String> medications) { this.medications = medications; }
}
```

- [ ] **Step 3: Create history model**

Create `PatientHealthUpdateHistory.java`:

```java
package com.medflow.model;

import lombok.*;
import java.time.OffsetDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PatientHealthUpdateHistory {
    private String id;
    private String patientId;
    private String patientName;
    private String recordId;
    private String doctorId;
    private String doctorName;
    private OffsetDateTime updateTimestamp;
    private String changes;
}
```

## Task 2: Repository Persistence

**Files:**
- Modify: `src/main/java/com/medflow/repository/UserRepository.java`

- [ ] **Step 1: Add repository methods**

Add public methods:

```java
public void addBloodPressure(String patientId, String label, int systolic, int diastolic)
public void addHeartRate(String patientId, String label, int rate)
public void replaceMedications(String patientId, List<String> medications)
public void insertHealthUpdateHistory(PatientHealthUpdateHistory history)
```

Implementation should use existing `database.getConnection()` and prepared statements only. Medication replacement should delete current `patient_medications` for the patient, then insert trimmed nonblank values.

- [ ] **Step 2: Verify method behavior by SQL inspection**

Run a grep to confirm all table names exist in schema and repository:

```powershell
rg -n "blood_pressure_history|heart_rate_history|patient_medications|patient_health_update_history" src\main
```

Expected: all four table names appear in `schema.sql` or `UserRepository.java`.

## Task 3: Record Service Rules

**Files:**
- Modify: `src/main/java/com/medflow/service/RecordService.java`

- [ ] **Step 1: Detailed edit history**

Replace generic `"Prontuario editado."` with a generated summary:

```java
private String describeRecordChanges(MedicalRecord before, EditRecordRequest req) {
    List<String> changes = new ArrayList<>();
    if (!same(before.getType(), req.getType())) changes.add("Tipo alterado de " + before.getType() + " para " + req.getType());
    if (!same(before.getDiagnosis(), req.getDiagnosis())) changes.add("Diagnostico alterado");
    if (!same(before.getRawNotes(), req.getRawNotes())) changes.add("Notas brutas alteradas");
    if (!same(before.getFormattedNotes(), req.getFormattedNotes())) changes.add("Notas formatadas alteradas");
    return changes.isEmpty() ? "Prontuario salvo sem alteracoes textuais." : String.join("; ", changes) + ".";
}

private boolean same(String left, String right) {
    return java.util.Objects.equals(left == null ? "" : left.trim(), right == null ? "" : right.trim());
}
```

- [ ] **Step 2: Add takeover operation**

Add:

```java
public MedicalRecord takeOver(String recordId)
```

Rules:
- current user must be doctor;
- record exists;
- if already responsible, return record without duplicating ownership change;
- update `doctorId` and `doctorName`;
- append `RecordEditHistory` with message `Prontuario assumido por Dr(a). X. Responsavel anterior: Y.`;
- write audit log action `RECORD_TAKEOVER` with warning severity.

- [ ] **Step 3: Add health update operation**

Add:

```java
public User updatePatientHealth(String patientId, UpdatePatientHealthRequest req)
```

Rules:
- `recordId` required;
- doctor authenticated;
- patient exists and has role `patient`;
- record exists and belongs to patient;
- type must be `consulta`, `exame`, or `cirurgia`;
- at least one of pressure, heart rate, medications provided;
- insert new pressure item when systolic and diastolic are present;
- insert new heart rate item when provided;
- replace medications when list is present;
- insert `PatientHealthUpdateHistory`;
- write audit log action `PATIENT_HEALTH_UPDATE`;
- return updated patient via `userRepo.findById(patientId)`.

## Task 4: HTTP Routes

**Files:**
- Modify: `src/main/java/com/medflow/MedFlowApplication.java`

- [ ] **Step 1: Import DTO**

Add:

```java
import com.medflow.dto.UpdatePatientHealthRequest;
```

- [ ] **Step 2: Add takeover route**

Near record routes:

```java
routes.add(new Route("PATCH", "/api/records/{id}/take-over", List.of("doctor"), 200,
        req -> recordService.takeOver(req.pathParam("id"))));
```

- [ ] **Step 3: Add patient health update route**

Near patient routes:

```java
routes.add(new Route("POST", "/api/patients/{id}/health-updates", List.of("doctor"), 200, req -> {
    UpdatePatientHealthRequest body = readBody(req, UpdatePatientHealthRequest.class);
    return AuthService.toDto(recordService.updatePatientHealth(req.pathParam("id"), body));
}));
```

## Task 5: Frontend API Client

**Files:**
- Modify: `src/main/resources/static/js/app.js`

- [ ] **Step 1: Add client methods**

Add:

```javascript
async function takeOverRecord(recordId) {
  return patch("/records/" + recordId + "/take-over");
}

async function updatePatientHealth(patientId, body) {
  return post("/patients/" + patientId + "/health-updates", body);
}
```

Expose them in the return object near record/patient methods.

## Task 6: Doctor Records UI

**Files:**
- Modify: `src/main/resources/static/pages/doctor/records.html`

- [ ] **Step 1: Show takeover action**

For records not owned by the current doctor, show:

```html
<button class="btn btn-warning btn-sm" onclick='takeOverRecord("RECORD_ID")'>
  <i data-lucide="user-check"></i> Assumir
</button>
```

- [ ] **Step 2: Add health update action**

For records whose patient is known, show:

```html
<button class="btn btn-secondary btn-sm" onclick='openHealthModal(RECORD_JSON)'>
  <i data-lucide="activity"></i> Atualizar saude
</button>
```

- [ ] **Step 3: Add modal fields**

Add modal with:
- patient name;
- record identifier/type;
- systolic;
- diastolic;
- heart rate;
- medications textarea with one medication per line.

- [ ] **Step 4: Submit health update**

Call:

```javascript
await App.updatePatientHealth(record.patientId, {
  recordId: record.id,
  systolic,
  diastolic,
  heartRate,
  medications,
});
```

Then close modal, toast success, and re-render.

## Task 7: Verification

**Files:**
- All modified files

- [ ] **Step 1: Compile/test**

Run:

```powershell
.\mvnw.cmd -B test
```

Expected when Java is configured: Maven exits 0. If `JAVA_HOME` is missing, record that verification is blocked by local environment.

- [ ] **Step 2: Static route check**

Run:

```powershell
rg -n "take-over|health-updates|RECORD_TAKEOVER|PATIENT_HEALTH_UPDATE|patient_health_update_history" src
```

Expected: all new route/action/table names appear.

- [ ] **Step 3: Manual browser/API smoke after deploy**

After merge/deploy:
- login as doctor;
- open prontuarios;
- view a record and confirm history appears;
- assume another doctor's record and confirm it becomes editable;
- update patient health data through a selected record;
- login as admin and confirm audit entries.
