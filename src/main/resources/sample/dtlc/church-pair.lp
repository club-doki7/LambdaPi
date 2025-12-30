defun pair = λt1. λt2. λx. λy. λtr. λf. f x y : ∀ (t1, t2 : *) -> t1 -> t2 -> ∀tr : * -> (t1 -> t2 -> tr) -> tr
defun car = λt1. λt2. λp. p t1 (λx. λy. x) : ∀ (t1, t2 : *) -> (∀tr : * -> (t1 -> t2 -> tr) -> tr) -> t1
defun cdr = λt1. λt2. λp. p t2 (λx. λy. y) : ∀ (t1, t2 : *) -> (∀tr : * -> (t1 -> t2 -> tr) -> tr) -> t2

axiom a, b : *
axiom x : a
axiom y : b

defun pair-a-b = pair a b
defun car-a-b = car a b
defun cdr-a-b = cdr a b

check pair-a-b x
defun p = pair-a-b x y

check car-a-b p
check cdr-a-b p
