|Case|n|m|Seed/generator|Algorithm|Run 1|Run 2|Run 3|Run 4|Run 5|Median|Correct outputs checked|
|---|---:|---:|---|---|---:|---:|---:|---:|---:|---:|---|
|random|300|30|java.util.Random(20261408), alphabet acgt, pattern = text[55..85)|naive|0.8|0.8|0.8|0.8|1.1|0.8|yes, 1 starts equal to String.indexOf reference|
|random|300|30|java.util.Random(20261408), alphabet acgt, pattern = text[55..85)|kmp|40.3|5.0|5.4|5.2|5.2|5.2|yes, 1 starts equal to String.indexOf reference|
|random|300|30|java.util.Random(20261408), alphabet acgt, pattern = text[55..85)|rk|4.2|4.1|45.0|4.6|4.3|4.3|yes, 1 starts equal to String.indexOf reference|
|random|1000|100|java.util.Random(20262108), alphabet acgt, pattern = text[406..506)|naive|2.9|2.6|2.4|2.4|2.6|2.6|yes, 1 starts equal to String.indexOf reference|
|random|1000|100|java.util.Random(20262108), alphabet acgt, pattern = text[406..506)|kmp|4.2|4.2|4.2|4.1|4.1|4.2|yes, 1 starts equal to String.indexOf reference|
|random|1000|100|java.util.Random(20262108), alphabet acgt, pattern = text[406..506)|rk|8.5|8.7|8.3|8.4|8.6|8.5|yes, 1 starts equal to String.indexOf reference|
|random|3000|300|java.util.Random(20264108), alphabet acgt, pattern = text[302..602)|naive|6.6|8.1|6.8|6.3|6.1|6.6|yes, 1 starts equal to String.indexOf reference|
|random|3000|300|java.util.Random(20264108), alphabet acgt, pattern = text[302..602)|kmp|9.9|11.0|11.8|11.6|7.9|11.0|yes, 1 starts equal to String.indexOf reference|
|random|3000|300|java.util.Random(20264108), alphabet acgt, pattern = text[302..602)|rk|25.7|25.2|25.1|27.1|28.1|25.7|yes, 1 starts equal to String.indexOf reference|
|all-a (repetitive)|300|30|text a^n, pattern a^m (every start matches)|naive|6.3|7.0|5.3|6.0|5.9|6.0|yes, 271 starts equal to String.indexOf reference|
|all-a (repetitive)|300|30|text a^n, pattern a^m (every start matches)|kmp|11.9|1.6|2.0|1.6|1.6|1.6|yes, 271 starts equal to String.indexOf reference|
|all-a (repetitive)|300|30|text a^n, pattern a^m (every start matches)|rk|16.3|16.6|16.2|17.0|16.3|16.3|yes, 271 starts equal to String.indexOf reference|
|all-a (repetitive)|1000|100|text a^n, pattern a^m (every start matches)|naive|136.0|121.5|109.3|110.5|107.1|110.5|yes, 901 starts equal to String.indexOf reference|
|all-a (repetitive)|1000|100|text a^n, pattern a^m (every start matches)|kmp|18.1|5.2|5.7|5.2|5.6|5.6|yes, 901 starts equal to String.indexOf reference|
|all-a (repetitive)|1000|100|text a^n, pattern a^m (every start matches)|rk|140.4|140.8|140.9|140.3|140.8|140.8|yes, 901 starts equal to String.indexOf reference|
|all-a (repetitive)|3000|300|text a^n, pattern a^m (every start matches)|naive|992.7|959.4|966.2|956.4|967.8|966.2|yes, 2701 starts equal to String.indexOf reference|
|all-a (repetitive)|3000|300|text a^n, pattern a^m (every start matches)|kmp|19.6|17.7|16.9|15.4|15.5|16.9|yes, 2701 starts equal to String.indexOf reference|
|all-a (repetitive)|3000|300|text a^n, pattern a^m (every start matches)|rk|1221.0|1213.1|1191.6|1189.5|1194.2|1194.2|yes, 2701 starts equal to String.indexOf reference|
|ab-periodic (repetitive)|300|30|text (ab)^(n/2), pattern (ab)^(m/2)|naive|5.2|5.4|5.6|5.6|6.0|5.6|yes, 136 starts equal to String.indexOf reference|
|ab-periodic (repetitive)|300|30|text (ab)^(n/2), pattern (ab)^(m/2)|kmp|1.6|1.3|1.3|1.2|1.2|1.3|yes, 136 starts equal to String.indexOf reference|
|ab-periodic (repetitive)|300|30|text (ab)^(n/2), pattern (ab)^(m/2)|rk|9.2|9.4|9.1|9.2|9.2|9.2|yes, 136 starts equal to String.indexOf reference|
|ab-periodic (repetitive)|1000|100|text (ab)^(n/2), pattern (ab)^(m/2)|naive|55.2|53.4|54.4|54.7|72.6|54.7|yes, 451 starts equal to String.indexOf reference|
|ab-periodic (repetitive)|1000|100|text (ab)^(n/2), pattern (ab)^(m/2)|kmp|3.8|3.7|3.9|3.8|4.0|3.8|yes, 451 starts equal to String.indexOf reference|
|ab-periodic (repetitive)|1000|100|text (ab)^(n/2), pattern (ab)^(m/2)|rk|72.5|73.1|72.8|72.7|73.9|72.8|yes, 451 starts equal to String.indexOf reference|
|ab-periodic (repetitive)|3000|300|text (ab)^(n/2), pattern (ab)^(m/2)|naive|502.0|483.6|491.1|482.9|502.0|491.1|yes, 1351 starts equal to String.indexOf reference|
|ab-periodic (repetitive)|3000|300|text (ab)^(n/2), pattern (ab)^(m/2)|kmp|13.4|12.1|12.7|12.1|32.3|12.7|yes, 1351 starts equal to String.indexOf reference|
|ab-periodic (repetitive)|3000|300|text (ab)^(n/2), pattern (ab)^(m/2)|rk|753.4|667.8|701.4|649.5|636.3|667.8|yes, 1351 starts equal to String.indexOf reference|
