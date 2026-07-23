$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$renderRoot = Join-Path $projectRoot 'scratch\office_render'
$slidesRoot = Join-Path $renderRoot 'slides'
New-Item -ItemType Directory -Force -Path $renderRoot | Out-Null
New-Item -ItemType Directory -Force -Path $slidesRoot | Out-Null

$reportPath = Join-Path $projectRoot 'docs\report.docx'
$reportPdf = Join-Path $renderRoot 'report.pdf'
$slidesPath = Join-Path $projectRoot 'docs\slide.pptx'
$slidesPdf = Join-Path $renderRoot 'slide.pdf'

$word = $null
$document = $null
try {
    $word = New-Object -ComObject Word.Application
    $word.Visible = $false
    $word.DisplayAlerts = 0
    $document = $word.Documents.Open($reportPath, $false, $true)
    $document.ExportAsFixedFormat($reportPdf, 17)
} finally {
    if ($null -ne $document) {
        try { $document.Close($false) } catch { Write-Warning "Word document close failed: $($_.Exception.Message)" }
    }
    if ($null -ne $word) {
        try { $word.Quit() } catch { Write-Warning "Word quit failed: $($_.Exception.Message)" }
    }
    if ($null -ne $document) { [void][Runtime.InteropServices.Marshal]::ReleaseComObject($document) }
    if ($null -ne $word) { [void][Runtime.InteropServices.Marshal]::ReleaseComObject($word) }
}

$powerPoint = $null
$presentation = $null
try {
    $powerPoint = New-Object -ComObject PowerPoint.Application
    $presentation = $powerPoint.Presentations.Open($slidesPath, $true, $false, $false)
    $presentation.SaveAs($slidesPdf, 32)
    $presentation.Export($slidesRoot, 'PNG', 1600, 900)
} finally {
    if ($null -ne $presentation) {
        try { $presentation.Close() } catch { Write-Warning "PowerPoint close failed: $($_.Exception.Message)" }
    }
    if ($null -ne $powerPoint) {
        try { $powerPoint.Quit() } catch { Write-Warning "PowerPoint quit failed: $($_.Exception.Message)" }
    }
    if ($null -ne $presentation) { [void][Runtime.InteropServices.Marshal]::ReleaseComObject($presentation) }
    if ($null -ne $powerPoint) { [void][Runtime.InteropServices.Marshal]::ReleaseComObject($powerPoint) }
}

Write-Output "Report PDF: $reportPdf"
Write-Output "Slide PDF: $slidesPdf"
Write-Output "Slide PNGs: $slidesRoot"
