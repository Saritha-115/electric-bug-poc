# RetailSync — Operations Guide

This covers how to redeploy, monitor, and maintain the RetailSync system:
a one-directional sync from a local Microsoft Access database (`recent.mdb`)
to a cloud PostgreSQL database (AWS RDS), served to the Electric Bug UI via
AWS Lambda + API Gateway.

---

## System overview

```
recent.mdb (local PC)
     │
     │  read-only, every 10 min (@Scheduled)
     ▼
retailsync-api (Windows Service, local PC)
     │
     │  upsert via JDBC
     ▼
RDS PostgreSQL (retailsync_cloud)
     ▲
     │  read-only queries
     │
retailsync-api (AWS Lambda, "lambda" profile)
     ▲
     │  HTTPS
     │
Electric Bug UI (React frontend)
```

Two deployments of the **same codebase**, running in different profiles:

| | Local Windows Service | AWS Lambda |
|---|---|---|
| Profile | `default` | `lambda` |
| Talks to `recent.mdb` | Yes | No |
| Talks to RDS | Yes (writes) | Yes (reads only) |
| Purpose | Sync mdb → RDS | Serve API to frontend |
| Runs via | WinSW service | SAM / CloudFormation |

---

## 1. Redeploying the AWS Lambda side

Whenever `retailsync-api` source code changes and you want the live API
updated:

```powershell
cd "D:\Aximly\Electric Bug\electric-bug-poc\retailsync-api"
sam build
sam deploy
```

- First-time deploy on a new machine/account: use `sam deploy --guided`
  instead, and save the answers to `samconfig.toml` when prompted — after
  that, plain `sam deploy` reuses those settings.
- After a successful deploy, the live API base URL is printed in the
  `Outputs` section of the terminal output (see `ProxyApiUrl`). It's also
  always retrievable via:

  ```powershell
  aws cloudformation describe-stack-resources --stack-name retailsync-api-poc --query "StackResources[?ResourceType=='AWS::ApiGateway::RestApi'].PhysicalResourceId" --output text
  ```

- To find the stack in the AWS Console reliably: go to **CloudFormation**
  (not API Gateway) → find the stack named exactly `retailsync-api-poc` →
  **Resources** tab → `ServerlessRestApi` → click through. This avoids
  confusion with any other unrelated API Gateway entries in the account.

- Test a live endpoint (always include a path — the bare root returns
  `Missing Authentication Token`, which is expected for a `{proxy+}` route
  with no path segment):

  ```powershell
  Invoke-RestMethod -Uri "https://nk9gi8npjg.execute-api.us-east-1.amazonaws.com/Prod/api/health"
  ```

  **Current live API base URL** (confirmed working):
  ```
  https://nk9gi8npjg.execute-api.us-east-1.amazonaws.com/Prod/api/
  ```
  This is the URL the React frontend (Electric Bug UI) should point at.
  Note the API ID (`nk9gi8npjg`) will change if the stack is ever deleted
  and redeployed from scratch — always re-verify with the
  `describe-stack-resources` command above rather than assuming this ID
  is permanent, and re-check the AWS Console isn't showing a different,
  unrelated API Gateway entry from another stack in the same account.

---

## 2. The local Windows Service

### Location

```
C:\RetailSync\
├── retailsync-api.jar          ← built via `mvn clean package`
├── application.properties      ← real credentials & mdb path, NOT in git
├── RetailSyncService.exe       ← WinSW
├── RetailSyncService.xml       ← service definition
└── logs\
    ├── RetailSyncService.out.log      ← stdout (Spring Boot logs, sync messages)
    ├── RetailSyncService.err.log      ← stderr (should normally be empty)
    └── RetailSyncService.wrapper.log  ← WinSW's own start/stop log
```

### Checking status

```powershell
Get-Service RetailSyncApi
```

Should show `Running`. If not:

```powershell
Get-Content C:\RetailSync\logs\RetailSyncService.wrapper.log
```

to see WinSW's own startup log, and:

```powershell
Get-Content C:\RetailSync\logs\RetailSyncService.out.log -Tail 50
```

to see the most recent application/sync output.

### Watching sync activity live

```powershell
Get-Content C:\RetailSync\logs\RetailSyncService.out.log -Wait
```

Every ~10 minutes you should see:

```
[CloudSyncService] Synced N stock rows
[CloudSyncService] Synced N customer rows
[CloudSyncService] Synced N layby rows
[CloudSyncService] Synced N payment rows
```

with no manual trigger required — this fires on its own via
`@Scheduled(fixedRate = 10 * 60 * 1000)` in `CloudSyncService.java`.

### Restarting the service

```powershell
cd C:\RetailSync
.\RetailSyncService.exe stop
.\RetailSyncService.exe start
```

### Reinstalling from scratch (e.g. after rebuilding the jar)

```powershell
cd C:\RetailSync
.\RetailSyncService.exe stop
.\RetailSyncService.exe uninstall
copy "D:\Aximly\Electric Bug\electric-bug-poc\retailsync-api\target\retailsync-api-0.0.1-SNAPSHOT.jar" "C:\RetailSync\retailsync-api.jar"
.\RetailSyncService.exe install
.\RetailSyncService.exe start
```

### Reboot behavior

The service is configured to start automatically on boot and restart on
failure (`<onfailure action="restart" delay="30 sec"/>` in
`RetailSyncService.xml`). No manual intervention should be needed after a
PC restart or temporary loss of internet/RDS connectivity — the scheduler
just quietly retries on its next cycle.

---

## 3. Rotating credentials

Do this periodically, and immediately if credentials are ever exposed
(e.g. pasted in a chat log, shared screen, committed to git).

### RDS master password

1. AWS Console → **RDS** → `aaapos-cloud` instance → **Modify** →
   set new master password → **Apply immediately**.
2. Update `C:\RetailSync\application.properties`:
   ```
   cloud.datasource.password=<new password>
   ```
3. Restart the local service:
   ```powershell
   cd C:\RetailSync
   .\RetailSyncService.exe stop
   .\RetailSyncService.exe start
   ```
4. Update the Lambda side — edit `samconfig.toml`'s
   `parameter_overrides` (or the relevant Secrets Manager entry, if
   migrated there) with the new password, then:
   ```powershell
   sam deploy
   ```

### `sync.apply-secret` (if write-back / apply-pending endpoints are still in use)

1. Generate a new random string.
2. Update it in `C:\RetailSync\application.properties`
   (`sync.apply-secret=...`).
3. Restart the local service (same as above).
4. Update any client/tool that calls the apply endpoint with the new
   `X-Apply-Secret` header value.

---

## 4. Known quirks / gotchas

- **PowerShell's `curl` is an alias for `Invoke-WebRequest`**, not real
  curl — it doesn't understand `-X`, `-H`, `-d` the way curl.exe does.
  Use `curl.exe` explicitly, or better, use `Invoke-RestMethod` with
  native PowerShell syntax.
- **PowerShell double-quoted strings don't use `\` as an escape
  character** — use single quotes for JSON bodies:
  `-Body '{"closed": true}'`
- **API Gateway's bare stage root returns `Missing Antml:parameter Token`**
  — this is normal for a `{proxy+}` route with no path segment after it,
  not an actual auth error. Always test with a real path
  (`/Prod/api/health`, etc.).
- **Access/Jet caches open tables** — if checking `recent.mdb` values in
  Microsoft Access right after a write, close and reopen the file (or
  requery) rather than trusting a table left open in Datasheet view.
- **Spring Boot upserts don't delete** — the sync uses
  `INSERT ... ON CONFLICT DO UPDATE`, so rows manually added to RDS but
  not present in `recent.mdb` will never be removed by the sync. This is
  intentional for a one-directional mdb → RDS design.

---

## 5. File locations quick reference

| What | Where |
|---|---|
| Source code | `D:\Aximly\Electric Bug\electric-bug-poc\retailsync-api` |
| SAM template | `...\retailsync-api\template.yaml` |
| SAM deploy config | `...\retailsync-api\samconfig.toml` |
| Local service install | `C:\RetailSync\` |
| Local service config | `C:\RetailSync\application.properties` |
| Local service logs | `C:\RetailSync\logs\` |
| `recent.mdb` (this PC) | `C:\RetailM\POC_TestShop\recent.mdb` |