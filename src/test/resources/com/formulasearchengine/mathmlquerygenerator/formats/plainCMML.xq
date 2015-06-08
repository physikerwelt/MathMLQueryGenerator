for $x in $m//*:apply
[*[1]/name() = 'eq' and *[2]/name() = 'apply' and *[2][*[1]/name() = 'times' and *[2]/name() = 'ci' and *[2][./text() = 'a'] and *[3]/name() = 'ci' and *[3][./text() = 'b']] and *[3]/name() = 'apply' and *[3][*[1]/name() = 'times' and *[2]/name() = 'ci' and *[2][./text() = 'b'] and *[3]/name() = 'ci' and *[3][./text() = 'a']]]
where
fn:count($x/*[2]/*[2]/*) = 0
 and fn:count($x/*[2]/*[3]/*) = 0
 and fn:count($x/*[2]/*) = 3
 and fn:count($x/*[3]/*[2]/*) = 0
 and fn:count($x/*[3]/*[3]/*) = 0
 and fn:count($x/*[3]/*) = 3
 and fn:count($x/*) = 3

return
