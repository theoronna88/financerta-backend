# Configuração de Variáveis de Ambiente

## Como usar variáveis de ambiente no projeto

### 1. Criar arquivo .env local

Copie o arquivo `.env.example` e crie um `.env`:

```bash
cp .env.example .env
```

### 2. Editar o arquivo .env com suas credenciais

Abra o arquivo `.env` e configure suas variáveis:

```properties
JWT_SECRET=sua-chave-secreta-aqui-deve-ter-pelo-menos-256-bits
JWT_EXPIRATION=86400000
```

**IMPORTANTE:** O arquivo `.env` está no `.gitignore` e NÃO será enviado para o repositório.

### 3. Como executar o projeto

#### Opção 1: Via IDE (IntelliJ IDEA)

1. Instale o plugin **EnvFile** (opcional)
2. Em Run/Debug Configurations, adicione as variáveis de ambiente

#### Opção 2: Via linha de comando (Maven)

**Windows (PowerShell):**
```powershell
$env:JWT_SECRET="sua-chave-secreta"
$env:JWT_EXPIRATION="86400000"
mvn spring-boot:run
```

**Linux/Mac:**
```bash
export JWT_SECRET="sua-chave-secreta"
export JWT_EXPIRATION="86400000"
mvn spring-boot:run
```

#### Opção 3: Via arquivo .env com Spring Boot

Se quiser carregar automaticamente o arquivo `.env`, adicione a dependência:

```xml
<dependency>
    <groupId>me.paulschwarz</groupId>
    <artifactId>spring-dotenv</artifactId>
    <version>4.0.0</version>
</dependency>
```

### 4. Em produção

Configure as variáveis de ambiente diretamente no servidor/container:

- **Docker:** Use arquivo `.env` ou `-e` no docker run
- **Kubernetes:** Use ConfigMaps ou Secrets
- **Cloud (AWS, Azure, etc.):** Configure nas variáveis de ambiente do serviço

### Gerar uma chave JWT segura

Para gerar uma chave JWT segura:

**PowerShell:**
```powershell
[Convert]::ToBase64String([System.Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
```

**Linux/Mac:**
```bash
openssl rand -base64 32
```

