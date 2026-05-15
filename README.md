# MedFlow

Aplicacao Java 17 com backend HTTP puro, PostgreSQL e frontend estatico servido pela propria aplicacao.

## Requisitos

- Java 17 instalado
- Acesso a um banco PostgreSQL
- Maven instalado ou Maven Wrapper do projeto

O projeto inclui Maven Wrapper (`mvnw` e `mvnw.cmd`), entao nao e obrigatorio instalar Maven na maquina.

## Configuracao

A aplicacao le configuracoes de variaveis de ambiente. Em desenvolvimento, tambem usa valores de `src/main/resources/application.properties`.

Variaveis principais:

```env
APP_DATASOURCE_URL=jdbc:postgresql://host:5432/database
APP_DATASOURCE_USERNAME=postgres
APP_DATASOURCE_PASSWORD=sua-senha
JWT_SECRET=um-segredo-grande-e-seguro
JWT_EXPIRATION_MS=86400000
CORS_ALLOWED_ORIGINS=http://localhost:8080
APP_MAIL_HOST=smtp.gmail.com
APP_MAIL_PORT=587
APP_MAIL_USERNAME=seu-email@gmail.com
APP_MAIL_PASSWORD=sua-senha-de-app
TWOFA_EXPIRY_MINUTES=5
```

Em hospedagens que fornecem a porta por ambiente, como alwaysdata, use tambem:

```env
PORT=8080
```

## Rodar Com Maven

Use esta opcao durante o desenvolvimento.

No Windows:

```powershell
.\mvnw.cmd clean package
java -jar target\medflow-backend-1.0.0.jar
```

Ou use o atalho:

```powershell
.\run-dev.cmd
```

No Linux/macOS:

```bash
./mvnw clean package
java -jar target/medflow-backend-1.0.0.jar
```

Se voce tiver Maven instalado, tambem pode usar:

```bash
mvn clean package
java -jar target/medflow-backend-1.0.0.jar
```

Depois de iniciar, acesse:

```text
http://localhost:8080
```

## Rodar Sem Maven

Use esta opcao quando a aplicacao ja tiver sido empacotada em `.jar`.

Primeiro, em uma maquina com Maven ou Maven Wrapper, gere o pacote:

```powershell
.\mvnw.cmd clean package -DskipTests
```

O arquivo gerado sera:

```text
target\medflow-backend-1.0.0.jar
```

Copie esse arquivo para a maquina/servidor de destino. Nessa maquina, basta ter Java 17 instalado e executar:

```bash
java -jar medflow-backend-1.0.0.jar
```

No Windows, se o JAR estiver na pasta `target`, voce tambem pode usar:

```powershell
.\run-jar.cmd
```

## Deploy Manual No alwaysdata

Gere o JAR localmente:

```powershell
.\mvnw.cmd clean package -DskipTests
```

Envie o arquivo abaixo por WinSCP/SFTP:

```text
target\medflow-backend-1.0.0.jar
```

Para a pasta no alwaysdata:

```text
/home/medflowjmira/medflow/medflow-backend-1.0.0.jar
```

No painel do alwaysdata, configure o site como:

```text
Type: User program
Working directory: /home/medflowjmira/medflow
Command: java -jar /home/medflowjmira/medflow/medflow-backend-1.0.0.jar
```

Depois configure as variaveis de ambiente no painel e reinicie o site.

## Comandos Uteis

Rodar testes:

```powershell
.\mvnw.cmd test
```

Gerar pacote:

```powershell
.\mvnw.cmd clean package -DskipTests
```

Executar JAR gerado:

```powershell
java -jar target\medflow-backend-1.0.0.jar
```

## Observacoes

- A pasta `target/` nao deve ser commitada; ela e gerada pelo Maven.
- Arquivos `.jar` nao devem ser commitados.
- Senhas, tokens e secrets devem ficar em variaveis de ambiente, nunca no codigo.
