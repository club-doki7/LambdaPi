defun CEq =
        λa. λx. λy. ∀ (P : a -> *) -> P x -> P y
    : ∀a : * -> a -> a -> *
defun crfl =
        λa. λx. λP. λpx. px
    : ∀a : * -> ∀x : a -> CEq a x x

defun ceq-trans =
        λa.
        λx. λy. λz.
        λeq-xy. λeq-yz.
        λP. λpx. eq-yz P (eq-xy P px)
    : ∀a : * -> ∀x : a -> ∀y : a -> ∀z : a -> CEq a x y -> CEq a y z -> CEq a x z

defun ceq-symm =
        λa. λx. λy. λeq-xy.
        eq-xy (λz. CEq a z x) (crfl a x)
    : ∀a : * -> ∀x : a -> ∀y : a -> CEq a x y -> CEq a y x

axiom m, n, p : *
axiom eq-mn : CEq * m n
axiom eq-np : CEq * n p
check ceq-trans * m n p eq-mn eq-np : CEq * m p

defun CNat = ∀a : * -> (a -> a) -> a -> a

defun czro = λa. λf. λx. x : CNat
defun csuc = λn. λa. λf. λx. f (n a f x) : CNat -> CNat

defun 0 = czro
defun 1 = csuc 0
defun 2 = csuc 1
defun 3 = csuc 2
defun 4 = csuc 3

defun + = λm. λn. λa. λf. λx. m a f (n a f x) : CNat -> CNat -> CNat

defun +-assoc = λm. λn. λp.
        crfl CNat (+ m (+ n p))
    : ∀ (m, n, p : CNat) -> CEq CNat (+ (+ m n) p) (+ m (+ n p))

check +-assoc 1 2 3 : CEq CNat (+ (+ 1 2) 3) (+ 1 (+ 2 3))
