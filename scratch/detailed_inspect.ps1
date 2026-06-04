$excelPath = "c:\Users\ASUS TUF\Downloads\Documents\LAB\shopeeconsole\NHOM_01_LAB211_FlashSale\ai_logs\LAB.AI_AuditLog_Template.xlsx"
$excel = New-Object -ComObject Excel.Application
$excel.Visible = $false
$excel.DisplayAlerts = $false

try {
    $workbook = $excel.Workbooks.Open($excelPath)
    
    # Sheet 1
    $sheet1 = $workbook.Sheets.Item(1)
    Write-Output "=== SHEET 1 ==="
    for ($r = 1; $r -le 40; $r++) {
        $vals = @()
        $hasVal = $false
        for ($c = 1; $c -le 10; $c++) {
            $val = $sheet1.Cells.Item($r, $c).Value2
            $vals += "$($c):$($val)"
            if ($val -ne $null) { $hasVal = $true }
        }
        if ($hasVal) { Write-Output "Row $($r): $($vals -join ' | ')" }
    }
    
    # Sheet 2
    $sheet2 = $workbook.Sheets.Item(2)
    Write-Output "=== SHEET 2 ==="
    for ($r = 1; $r -le 40; $r++) {
        $vals = @()
        $hasVal = $false
        for ($c = 1; $c -le 10; $c++) {
            $val = $sheet2.Cells.Item($r, $c).Value2
            $vals += "$($c):$($val)"
            if ($val -ne $null) { $hasVal = $true }
        }
        if ($hasVal) { Write-Output "Row $($r): $($vals -join ' | ')" }
    }

    # Sheet 3
    $sheet3 = $workbook.Sheets.Item(3)
    Write-Output "=== SHEET 3 ==="
    for ($r = 1; $r -le 40; $r++) {
        $vals = @()
        $hasVal = $false
        for ($c = 1; $c -le 10; $c++) {
            $val = $sheet3.Cells.Item($r, $c).Value2
            $vals += "$($c):$($val)"
            if ($val -ne $null) { $hasVal = $true }
        }
        if ($hasVal) { Write-Output "Row $($r): $($vals -join ' | ')" }
    }

    # Sheet 4
    $sheet4 = $workbook.Sheets.Item(4)
    Write-Output "=== SHEET 4 ==="
    for ($r = 1; $r -le 40; $r++) {
        $vals = @()
        $hasVal = $false
        for ($c = 1; $c -le 10; $c++) {
            $val = $sheet4.Cells.Item($r, $c).Value2
            $vals += "$($c):$($val)"
            if ($val -ne $null) { $hasVal = $true }
        }
        if ($hasVal) { Write-Output "Row $($r): $($vals -join ' | ')" }
    }

    $workbook.Close($false)
} catch {
    Write-Output "Error: $_"
} finally {
    $excel.Quit()
    [System.Runtime.Interopservices.Marshal]::ReleaseComObject($excel) | Out-Null
}
