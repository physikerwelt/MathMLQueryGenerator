for $x in $m//*:ci
[./text() = 'E']
where
fn:count($x/*) = 0

return
