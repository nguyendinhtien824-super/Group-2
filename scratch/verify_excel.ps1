$excelPath = "c:\Users\ASUS TUF\Downloads\Documents\LAB\shopeeconsole\NHOM_01_LAB211_FlashSale\ai_logs\LAB.AI_AuditLog_Template.xlsx"
$excel = New-Object -ComObject Excel.Application
$excel.Visible = $false
$excel.DisplayAlerts = $false

try {
    $workbook = $excel.Workbooks.Open($excelPath)
    
    Write-Output "==========================================="
    Write-Output "VERIFYING SHEET 1: Metadata & Summary"
    $sheet1 = $workbook.Sheets.Item(1)
    Write-Output "Student Name: $($sheet1.Cells.Item(4, 3).Value2)"
    Write-Output "Student ID: $($sheet1.Cells.Item(5, 3).Value2)"
    Write-Output "Total Prompts: $($sheet1.Cells.Item(10, 3).Value2)"
    Write-Output "Core Prompts: $($sheet1.Cells.Item(11, 3).Value2)"
    Write-Output "Selection Ratio: $($sheet1.Cells.Item(12, 3).Value2) (Formula: $($sheet1.Cells.Item(12, 3).Formula))"
    Write-Output "Hallucinations: $($sheet1.Cells.Item(13, 3).Value2)"
    Write-Output "DTC Decomposition: $($sheet1.Cells.Item(24, 2).Value2)"
    Write-Output "DTC Pattern: $($sheet1.Cells.Item(25, 2).Value2)"
    Write-Output "DTC Abstraction: $($sheet1.Cells.Item(26, 2).Value2)"
    Write-Output "DTC Algorithms: $($sheet1.Cells.Item(27, 2).Value2)"
    
    Write-Output "--- AI Tools Used List ---"
    for ($r = 16; $r -le 21; $r++) {
        $tool = $sheet1.Cells.Item($r, 1).Value2
        $purpose = $sheet1.Cells.Item($r, 2).Value2
        $freq = $sheet1.Cells.Item($r, 3).Value2
        $val = $sheet1.Cells.Item($r, 4).Value2
        Write-Output "Row $($r): Tool='$tool' | Purpose='$purpose' | Freq='$freq' | Value='$val'"
    }
    
    Write-Output "==========================================="
    Write-Output "VERIFYING SHEET 2: Detailed Audit Log"
    $sheet2 = $workbook.Sheets.Item(2)
    for ($r = 4; $r -le 8; $r++) {
        $id = $sheet2.Cells.Item($r, 1).Value2
        $type = $sheet2.Cells.Item($r, 2).Value2
        $stage = $sheet2.Cells.Item($r, 3).Value2
        $prob = $sheet2.Cells.Item($r, 4).Value2
        Write-Output "Row $($r) - ID='$id' | Type='$type' | Stage='$stage' | Problem='$prob'"
    }

    Write-Output "==========================================="
    Write-Output "VERIFYING SHEET 3: Hallucination Detection"
    $sheet3 = $workbook.Sheets.Item(3)
    for ($r = 4; $r -le 6; $r++) {
        $id = $sheet3.Cells.Item($r, 1).Value2
        $type = $sheet3.Cells.Item($r, 2).Value2
        $claim = $sheet3.Cells.Item($r, 3).Value2
        Write-Output "Row $($r) - ID='$id' | Type='$type' | Claim='$claim'"
    }

    Write-Output "==========================================="
    Write-Output "VERIFYING SHEET 4: Self-Assessment Checklist"
    $sheet4 = $workbook.Sheets.Item(4)
    Write-Output "Checklist A Row 6 Col 3: $($sheet4.Cells.Item(6, 3).Value2)"
    Write-Output "Checklist B Row 14 Col 3: $($sheet4.Cells.Item(14, 3).Value2), Col 4: $($sheet4.Cells.Item(14, 4).Value2)"
    Write-Output "Vivas Prep C Row 28 Cols 2-4: $($sheet4.Cells.Item(28, 2).Value2) | $($sheet4.Cells.Item(28, 3).Value2) | $($sheet4.Cells.Item(28, 4).Value2)"
    Write-Output "==========================================="
    
    $workbook.Close($false)
} catch {
    Write-Output "Error: $_"
} finally {
    $excel.Quit()
    [System.Runtime.Interopservices.Marshal]::ReleaseComObject($excel) | Out-Null
}
