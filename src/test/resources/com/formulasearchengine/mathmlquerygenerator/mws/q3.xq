for $x in $m//*:apply
[*[1]/name() = 'sin' and *[2]/name() = 'ci' and *[2][./text() = 'x']]
where
fn:count($x/*[2]/*) = 0
 and fn:count($x/*) = 2

return
