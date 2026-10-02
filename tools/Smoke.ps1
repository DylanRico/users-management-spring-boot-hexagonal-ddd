param([string]$BaseUrl = 'http://localhost:8080')
$ErrorActionPreference = 'Stop'
function Assert-That([bool]$condition, [string]$message) {
    if (-not $condition) { throw $message }
}
function Send-Json([string]$method, [string]$path, $body, $headers = @{}) {
    $options = @{ Method = $method; Uri = ($BaseUrl.TrimEnd('/') + $path); Headers = $headers }
    if ($null -ne $body) { $options.ContentType = 'application/json'; $options.Body = ($body | ConvertTo-Json) }
    Invoke-RestMethod @options
}
if (-not $env:ADMIN_EMAIL -or -not $env:ADMIN_PASSWORD -or -not $env:TEST_EMAIL) { throw 'Define ADMIN_EMAIL, ADMIN_PASSWORD y TEST_EMAIL; no se imprimen credenciales.' }
$deadline = [DateTime]::UtcNow.AddSeconds(60)
do {
    try {
        $docs = Invoke-RestMethod ($BaseUrl.TrimEnd('/') + '/v3/api-docs') -TimeoutSec 5
        break
    } catch {
        if ([DateTime]::UtcNow -ge $deadline) { throw 'La API no estuvo disponible en 60 segundos' }
        Start-Sleep -Seconds 1
    }
} while ($true)
Assert-That ([bool]$docs.paths.'/api/users') 'Swagger no describe la API'
Write-Output 'PASS Swagger /v3/api-docs'
$admin = Send-Json 'POST' '/api/auth/login' @{email = $env:ADMIN_EMAIL; password = $env:ADMIN_PASSWORD}
Assert-That ([bool]$admin.accessToken) 'Login de administrador sin JWT'
$headers = @{Authorization = ('Bearer ' + $admin.accessToken)}
Write-Output 'PASS Login administrador y JWT'
$id = [Guid]::NewGuid().ToString()
$password = 'Taller_' + [Guid]::NewGuid().ToString('N')
$user = Send-Json 'POST' '/api/users' @{id=$id; name='Usuario Taller'; email=$env:TEST_EMAIL; password=$password; role='MEMBER'}
Assert-That ($user.id -eq $id -and $user.status -eq 'PENDING') 'Registro no creo el usuario pendiente'
Write-Output 'PASS Registro MEMBER con estado PENDING'
$rejected = $false
try { Send-Json 'POST' '/api/auth/login' @{email=$env:TEST_EMAIL; password=$password} | Out-Null } catch { $rejected = [int]$_.Exception.Response.StatusCode -eq 401 }
Assert-That $rejected 'El usuario pendiente pudo iniciar sesion'
Write-Output 'PASS Login PENDING rechazado (401)'
$active = Send-Json 'PUT' ('/api/users/' + $id) @{name='Usuario Taller';email=$env:TEST_EMAIL;role='MEMBER';status='ACTIVE'} $headers
Assert-That ($active.status -eq 'ACTIVE') 'Activacion por ADMIN fallo'
$login = Send-Json 'POST' '/api/auth/login' @{email=$env:TEST_EMAIL;password=$password}
Assert-That ([bool]$login.accessToken) 'El usuario activo no obtuvo JWT'
Write-Output 'PASS Activacion ADMIN y login MEMBER'
$wrongPassword = $false
try { Send-Json 'POST' '/api/auth/login' @{email=$env:TEST_EMAIL;password='Incorrecta123'} | Out-Null } catch { $wrongPassword = [int]$_.Exception.Response.StatusCode -eq 401 }
Assert-That $wrongPassword 'Una contrasena incorrecta fue aceptada'
Write-Output 'PASS Contrasena incorrecta rechazada (401)'
$duplicate = $false
try { Send-Json 'POST' '/api/users' @{id=[Guid]::NewGuid().ToString();name='Duplicado Taller';email=$env:TEST_EMAIL;password=$password;role='MEMBER'} | Out-Null } catch { $duplicate = [int]$_.Exception.Response.StatusCode -eq 409 }
Assert-That $duplicate 'El correo duplicado no fue rechazado'
Write-Output 'PASS Correo duplicado rechazado (409)'
$users = Send-Json 'GET' '/api/users' $null $headers
Assert-That ([bool]($users | Where-Object {$_.id -eq $id})) 'El usuario no aparece en consulta protegida'
Write-Output ('PASS Consulta protegida; usuarios=' + $users.Count)
Write-Output 'Smoke completo; usuario conservado para verificar migracion.'
