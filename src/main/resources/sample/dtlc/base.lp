defun Eq =
        λa. λx. λy. ∀ (P : a -> *) -> P x -> P y
    : ∀a : * -> a -> a -> *
defun refl =
        λa. λx. λP. λpx. px
    : ∀a : * -> ∀x : a -> Eq a x x

defun eq-trans =
        λa.
        λx. λy. λz.
        λeq-xy. λeq-yz.
        λP. λpx. eq-yz P (eq-xy P px)
    : ∀a : * -> ∀x : a -> ∀y : a -> ∀z : a -> Eq a x y -> Eq a y z -> Eq a x z

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
        refl CNat (+ m (+ n p))
    : ∀ (m, n, p : CNat) -> Eq CNat (+ (+ m n) p) (+ m (+ n p))

check +-assoc 1 2 3 : Eq CNat (+ (+ 1 2) 3) (+ 1 (+ 2 3))

axiom m, n, p : CNat
axiom eq-mn : Eq CNat m n
axiom eq-np : Eq CNat n p

check eq-trans CNat m n p eq-mn eq-np : Eq CNat m p
