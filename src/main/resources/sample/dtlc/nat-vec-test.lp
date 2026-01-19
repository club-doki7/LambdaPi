axiom a : *
axiom x, y, z : a

defun zxx = cons a 2 z (cons a 1 x (cons a 0 x (nil a)))
defun one_y = cons a 0 y (nil a)
defun zy = cons a 1 z (cons a 0 y (nil a))

check append a 4 (append a 3 zxx 1 one_y) 2 zy
