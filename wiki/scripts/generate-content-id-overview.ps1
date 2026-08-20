param(
    [string]$ContentDirectory = (Join-Path $PSScriptRoot "../content"),
    [string]$OutputFile = (Join-Path $PSScriptRoot "../content-id-overview.md")
)

$contentRoot = (Resolve-Path -LiteralPath $ContentDirectory).Path
$idPattern = '[a-z0-9_.-]+:[a-z0-9_./-]+'
$entries = foreach ($file in Get-ChildItem -LiteralPath $contentRoot -Recurse -File -Filter '*.mdx') {
    $source = Get-Content -LiteralPath $file.FullName -Raw
    if ($source -notmatch '(?s)^---\s*\r?\n(?<frontMatter>.*?)\r?\n---') {
        throw "Missing front matter in $($file.FullName)"
    }

    $frontMatter = $Matches.frontMatter
    if ($frontMatter -notmatch '(?m)^id:\s*(?<id>[^\r\n]+)') {
        throw "Missing id in $($file.FullName)"
    }

    $id = [regex]::Match($Matches.id, $idPattern).Value
    if (-not $id) {
        throw "Invalid id in $($file.FullName)"
    }

    $relatedItems = @()
    if ($frontMatter -match '(?m)^related_items:\s*(?<items>[^\r\n]*)') {
        $relatedItems = @([regex]::Matches($Matches.items, $idPattern) | ForEach-Object Value)
    }

    [pscustomobject]@{
        Path = [IO.Path]::GetRelativePath($contentRoot, $file.FullName).Replace('\', '/')
        Id = $id
        RelatedItems = $relatedItems
    }
}

$lines = [Collections.Generic.List[string]]::new()
$lines.Add('# Wiki content ID overview')
$lines.Add('')
$lines.Add('Generated from the front matter of canonical pages in `wiki/content`. An em dash means that the page has no `related_items` field.')
$lines.Add('')

foreach ($entry in $entries | Sort-Object Path) {
    $relatedItems = if ($entry.RelatedItems.Count) {
        ($entry.RelatedItems | ForEach-Object { "``$_``" }) -join ', '
    } else {
        '—'
    }

    $lines.Add("- ``$($entry.Path)`` — id: ``$($entry.Id)``; related_items: $relatedItems")
}

$lines.Add('')
Set-Content -LiteralPath $OutputFile -Value $lines -Encoding utf8
