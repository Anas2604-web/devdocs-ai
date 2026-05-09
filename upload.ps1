Add-Type -AssemblyName System.Net.Http

$token = $TOKEN
$filePath = (Get-Item "petstore.yaml").FullName

$client = New-Object System.Net.Http.HttpClient
$client.DefaultRequestHeaders.Add("Authorization", "Bearer $token")

$content = New-Object System.Net.Http.MultipartFormDataContent
$fileStream = [System.IO.File]::OpenRead($filePath)
$fileContent = New-Object System.Net.Http.StreamContent($fileStream)
$fileContent.Headers.ContentType = [System.Net.Http.Headers.MediaTypeHeaderValue]::Parse("application/octet-stream")
$content.Add($fileContent, "file", "petstore.yaml")
$content.Add((New-Object System.Net.Http.StringContent("PetStore API")), "name")

$response = $client.PostAsync("http://localhost:9090/api/specs/upload", $content).Result
$body = $response.Content.ReadAsStringAsync().Result
Write-Host $body

$fileStream.Close()
$client.Dispose()