defun Eq = λa. λx. λy. (∀P : (a -> *) -> P x -> P y) : ∀a : * -> a -> a -> *
defun refl = λa. λx. λP. λpx. px : ∀a : * -> ∀x : a -> Eq a x x

axiom A : *
axiom m, n, p : A
axiom m-is-n : Eq A m n
axiom n-is-p : Eq A n p

defun trans = λa. λx. λy. λz. λeq_xy. λeq_yz. λP. λpx. eq_yz P (eq_xy P px) : ∀a : * -> ∀x : a -> ∀y : a -> ∀z : a -> Eq a x y -> Eq a y z -> Eq a x z
check trans A m n p m-is-n n-is-p : Eq A m p