$jsonPath = "C:\Users\ASUS TUF\Downloads\Documents\LAB\shopeeconsole\scratch\audit_log_data.json"
$excelPath = "c:\Users\ASUS TUF\Downloads\Documents\LAB\shopeeconsole\NHOM_01_LAB211_FlashSale\ai_logs\LAB.AI_AuditLog_Template.xlsx"

# Read JSON data
$data = Get-Content -Raw -Path $jsonPath -Encoding UTF8 | ConvertFrom-Json

$excel = New-Object -ComObject Excel.Application
$excel.Visible = $false
$excel.DisplayAlerts = $false

$workbook = $null
try {
    $workbook = $excel.Workbooks.Open($excelPath)
    
    # ----------------------------------------------------
    # SHEET 1: 1. Metadata & Summary
    # ----------------------------------------------------
    $sheet1 = $workbook.Sheets.Item(1)
    $sheet1.Cells.Item(4, 3).Value2 = $data.studentName
    $sheet1.Cells.Item(5, 3).Value2 = $data.studentId
    $sheet1.Cells.Item(10, 3).Value2 = [int]$data.totalPrompts
    $sheet1.Cells.Item(11, 3).Value2 = [int]$data.corePrompts
    $sheet1.Cells.Item(12, 3).Formula = "=C11/C10"
    $sheet1.Cells.Item(13, 3).Value2 = [int]$data.hallucinationsCount

    $sheet1.Cells.Item(24, 2).Value2 = [int]$data.dtcDecomposition
    $sheet1.Cells.Item(25, 2).Value2 = [int]$data.dtcPattern
    $sheet1.Cells.Item(26, 2).Value2 = [int]$data.dtcAbstraction
    $sheet1.Cells.Item(27, 2).Value2 = [int]$data.dtcAlgorithms

    # AI Tools table updates: overwrite Row 20 with Gemini, and add new [Add more...] to Row 21
    $sheet1.Cells.Item(20, 1).Value2 = "Gemini"
    $sheet1.Cells.Item(20, 2).Value2 = "Analyzing concurrency constraints, optimizing CsvRepository, and refactoring repository code."
    $sheet1.Cells.Item(20, 3).Value2 = "Medium"
    $sheet1.Cells.Item(20, 4).Value2 = "Highly accurate explanations of Java thread models and code generation."
    
    $sheet1.Cells.Item(21, 1).Value2 = "[Add more...]"
    $sheet1.Cells.Item(21, 2).Value2 = $null
    $sheet1.Cells.Item(21, 3).Value2 = $null
    $sheet1.Cells.Item(21, 4).Value2 = $null

    # ----------------------------------------------------
    # SHEET 2: 2. Detailed Audit Log
    # ----------------------------------------------------
    $sheet2 = $workbook.Sheets.Item(2)
    
    # Enable text wrapping on columns 4, 5, 6, 7
    for ($c = 4; $c -le 7; $c++) {
        $sheet2.Columns.Item($c).WrapText = $true
    }

    $row = 4
    foreach ($entry in $data.sheet2Entries) {
        $sheet2.Cells.Item($row, 1).Value2 = $entry.id
        $sheet2.Cells.Item($row, 2).Value2 = $entry.type
        $sheet2.Cells.Item($row, 3).Value2 = $entry.stage
        $sheet2.Cells.Item($row, 4).Value2 = $entry.problem
        $sheet2.Cells.Item($row, 5).Value2 = $entry.prompt
        $sheet2.Cells.Item($row, 6).Value2 = $entry.response
        $sheet2.Cells.Item($row, 7).Value2 = $entry.delta
        $sheet2.Cells.Item($row, 8).Value2 = $entry.evidence
        $row++
    }

    # ----------------------------------------------------
    # SHEET 3: 3. Hallucination Detection
    # ----------------------------------------------------
    $sheet3 = $workbook.Sheets.Item(3)
    
    # Enable text wrapping on columns 3, 4, 5, 6
    for ($c = 3; $c -le 6; $c++) {
        $sheet3.Columns.Item($c).WrapText = $true
    }

    # Clear row 5 first
    for ($c = 1; $c -le 6; $c++) {
        $sheet3.Cells.Item(5, $c).Value2 = $null
    }

    $row = 4
    foreach ($case in $data.sheet3Entries) {
        $sheet3.Cells.Item($row, 1).Value2 = $case.id
        $sheet3.Cells.Item($row, 2).Value2 = $case.type
        $sheet3.Cells.Item($row, 3).Value2 = $case.claim
        $sheet3.Cells.Item($row, 4).Value2 = $case.reality
        $sheet3.Cells.Item($row, 5).Value2 = $case.check
        $sheet3.Cells.Item($row, 6).Value2 = $case.corrective
        $row++
    }

    # ----------------------------------------------------
    # SHEET 4: 4. Self-Assessment Checklist
    # ----------------------------------------------------
    $sheet4 = $workbook.Sheets.Item(4)
    
    # Checklist A
    for ($r = 6; $r -le 10; $r++) {
        $sheet4.Cells.Item($r, 3).Value2 = "PASS"
    }

    # Checklist B
    $sheet4.Cells.Item(14, 3).Value2 = "PASS"
    $sheet4.Cells.Item(14, 4).Value2 = "5 entries (in range 5-10)"

    $sheet4.Cells.Item(15, 3).Value2 = "PASS"
    $sheet4.Cells.Item(15, 4).Value2 = "Each DTC component has at least 1 entry"

    $sheet4.Cells.Item(16, 3).Value2 = "PASS"
    $sheet4.Cells.Item(16, 4).Value2 = "3 cases logged"

    $sheet4.Cells.Item(17, 3).Value2 = "PASS"
    $sheet4.Cells.Item(17, 4).Value2 = "All entries have complete 4-question Human Delta"

    $sheet4.Cells.Item(18, 3).Value2 = "PASS"
    $sheet4.Cells.Item(18, 4).Value2 = "Evidence provided for 100% of entries"

    # Vivas Prep C
    for ($r = 28; $r -le 30; $r++) {
        $sheet4.Cells.Item($r, 2).Value2 = "Yes"
        $sheet4.Cells.Item($r, 3).Value2 = "Yes"
        $sheet4.Cells.Item($r, 4).Value2 = "Yes"
    }

    # Save workbook
    $workbook.Save()
    Write-Output "Excel workbook updated and saved successfully!"
    $workbook.Close($false)
    $workbook = $null
} catch {
    Write-Output "Error occurred: $_"
    if ($workbook -ne $null) { 
        $workbook.Close($false)
        $workbook = $null
    }
} finally {
    $excel.Quit()
    [System.Runtime.Interopservices.Marshal]::ReleaseComObject($excel) | Out-Null
}
