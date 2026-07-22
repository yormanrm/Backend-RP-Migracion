# Dudas técnicas 4 — Un solo punto suelto

Revisé `dudastecnicas3.md` completo. Los puntos 1 a 4 quedaron totalmente claros. En el punto 5 (reporte de ventas) respondiste 3 de las 4 preguntas que hice — quedó una suelta:

---

## 1. "Producto menos vendido": ¿cuentan los que tienen 0 ventas?

Ya confirmaste que las cifras son históricas por defecto (con opción de generarlas por rango también) y que el stock total es en tiempo real. Falta solo esto: si un asociado tiene productos que nunca se han vendido (`salesCount = 0`), y hay varios empatados en 0, ¿el reporte debe:

- a) Mostrar cualquiera de esos productos en 0 como "el menos vendido" (el primero que encuentre, sin criterio especial de desempate).
- b) Excluir del ranking de "menos vendido" a los productos con 0 ventas — el campo solo aplica entre productos que ya vendieron al menos una vez. Si *todos* sus productos tienen 0 ventas, el campo simplemente viene `null` o vacío.

Recomiendo la **(b)**: es más útil para el asociado ("tu producto que menos rota, entre los que sí venden, es X") que devolver un producto al azar entre varios con cero ventas.

R = Si, esta bien la opcion B. Incluso podemos devolver en un arreglo en el reporte esos productos que no tienen ni una sola venta, eso le serviria para saber si retirarlos o no

---

Todo lo demás de `dudastecnicas3.md` (marcas por autoservicio, endpoints de marca, estado agregado de la orden padre calculado en backend) quedó resuelto sin dudas adicionales. En cuanto respondas este punto, armo `planificaciondemejoras.md` con el plan completo.
