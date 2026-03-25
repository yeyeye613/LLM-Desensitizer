
$text = @"
这里是一段包含各种敏感信息的测试文本。
我的手机号是 13812345678，备用电话 13987654321。
请将邮件发送至 test_user@example.com 或 admin@company.org。
我的身份证号是 110101199003071233，出生日期是 1990-03-07。
工资卡号（招商银行）是 6222026006705353 ，信用卡（Visa）是 4000123456789010。
我的护照号码是 E12345678。
美国的社会安全码是 123-45-6789。
API密钥是 sk-1234567890abcdef1234567890abcdef，请勿泄露。
数据库密码是 MySecretP@ssw0rd!。
服务器IP地址是 192.168.1.100。
我的车牌号是 京A88888。
"@

$body = @{
    content = $text
    dataType = "TEXT"
    strategy = "maskDesensitizationStrategy"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/desensitize/text" -Method Post -Body $body -ContentType "application/json" -ErrorAction Stop
    Write-Host "原始文本："
    Write-Host $text
    Write-Host "`n----------------------------------------`n"
    Write-Host "脱敏后文本："
    Write-Host $response.maskedContent
    
    Write-Host "`n完整响应："
    $response | ConvertTo-Json -Depth 5
} catch {
    Write-Error "请求失败: $_"
    Write-Host "尝试检查端口..."
    Get-NetTCPConnection -State Listen | Where-Object { $_.OwningProcess -eq (Get-Process -Name "java" -ErrorAction SilentlyContinue).Id }
}
