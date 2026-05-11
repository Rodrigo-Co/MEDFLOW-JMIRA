# MedFlow Backend

Este projeto ficou preparado para funcionar de 2 jeitos:

1. com Maven Wrapper, sem precisar instalar Maven na maquina
2. so com o `.jar` gerado, sem Maven e sem wrapper

## Opcao 1: rodar com Maven Wrapper

Use esta opcao quando voce estiver desenvolvendo o projeto.

No Windows:

```powershell
.\mvnw.cmd clean package
java -jar target\medflow-backend-1.0.0.jar
```

Ou, se preferir, use o atalho:

```powershell
.\run-dev.cmd
```

No Linux/macOS:

```bash
./mvnw clean package
java -jar target/medflow-backend-1.0.0.jar
```

Importante:

- nao precisa instalar Maven
- na primeira vez, o wrapper pode baixar o Maven automaticamente
- as dependencias do projeto tambem podem ser baixadas na primeira execucao

## Opcao 2: rodar so com o JAR

Use esta opcao quando voce quiser executar a aplicacao em outra maquina sem Maven.

Primeiro gere o pacote:

```powershell
.\mvnw.cmd clean package
```

Depois rode o `.jar`:

```powershell
java -jar target\medflow-backend-1.0.0.jar
```

Ou use o atalho:

```powershell
.\run-jar.cmd
```

Importante:

- nessa opcao a outra maquina precisa so do Java
- nao precisa instalar Maven
- nao precisa baixar Maven para executar o `.jar`

## Comandos uteis

Rodar testes:

```powershell
.\mvnw.cmd test
```

Gerar o pacote:

```powershell
.\mvnw.cmd clean package
```

## Requisito

Voce ainda precisa ter o Java instalado. O `pom.xml` do projeto esta configurado para Java 17.

## Sem baixar nada

Tem como, mas somente no fluxo do `.jar`.

Se a ideia for rodar em outra maquina sem baixar nada:

1. gere o `.jar` em uma maquina que ja tenha baixado tudo
2. copie a pasta `target` ou pelo menos o arquivo `.jar`
3. execute com `java -jar`

Se quiser compilar com wrapper totalmente offline, alem do Maven tambem seria preciso levar as dependencias Maven ja em cache. Isso e possivel, mas fica mais pesado e menos pratico do que distribuir o `.jar`.
