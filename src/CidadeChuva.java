import java.util.Random;
import java.util.Scanner;

public class CidadeChuva {


    // Soma os RMs de todo mundo do grupo e vira o "RM da equipe". Fizemos isso porque
    // o resto do programa inteiro (tamanho da grade, semente do Random, célula do D1/D2)
    // sai desse número, então cada grupo cai numa cidade diferente, mas sempre a mesma
    // se usar os mesmos RMs.
    static int calcularRmEquipe(int[] rms) {
        int soma = 0;
        for (int i = 0; i < rms.length; i++) {
            soma += rms[i];
        }
        return soma;
    }


    // Pega o RM e tira dele os 5 parâmetros da simulação, usando resto da divisão e
    // dígitos diferentes (unidade, dezena, centena) pra não ficar tudo igual.
    // p[0]=N linhas, p[1]=M colunas, p[2]=K ciclos, p[3]=limiar do alerta e
    // p[4]=ordem dos vizinhos (0 ou 1). Somamos uma base em quase todos pra grade
    // e os ciclos não ficarem pequenos demais.
    static int[] calcularParametros(int rm) {
        int[] p = new int[5];
        p[0] = 8 + rm % 3;
        p[1] = 10 + (rm / 10) % 3;
        p[2] = 4 + (rm / 100) % 3;
        p[3] = 10 + 2 * (rm % 4);
        p[4] = rm % 2;
        return p;
    }


    // Monta o relevo da cidade. A altitude é 3 vezes a distância até a linha do meio,
    // mais um aleatório de 0 a 4. Assim o meio vira um vale e as bordas ficam mais altas,
    // então a água escorre pro centro, e o aleatório deixa o terreno mais "real".
    static int[][] gerarAltitude(Random gerador, int n, int m) {
        int[][] altitude = new int[n][m];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                altitude[i][j] = 3 * Math.abs(i - n / 2) + gerador.nextInt(5);
            }
        }
        return altitude;
    }


    // Cria uma matriz n x m com números sorteados entre minimo e maximo (inclusive).
    // A gente usa pra chuva inicial (0 a 10 cm em cada célula). Passamos o mesmo
    // Random do RM pra dar sempre o mesmo resultado.
    static int[][] gerarMatriz(Random gerador, int n, int m, int minimo, int maximo) {
        int[][] matriz = new int[n][m];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                matriz[i][j] = minimo + gerador.nextInt(maximo - minimo + 1);
            }
        }
        return matriz;
    }


    // Devolve quanto somar na linha pra ir pra cada vizinho (cima, direita, baixo,
    // esquerda ou a outra ordem). A ordem muda com o RM porque, se dois vizinhos
    // empatam, o primeiro que a gente olha ganha, então ela mexe no caminho da água.
    // Deixamos num vetor pra percorrer com for em vez de escrever 4 ifs.
    static int[] criarDeslocamentoLinha(int ordem) {
        if (ordem == 0) {
            return new int[]{-1, 0, 1, 0};
        }
        return new int[]{0, 1, 0, -1};
    }


    // Parceiro do de cima: mesmo esquema, mas pra coluna. Os dois vetores têm que andar
    // juntos (mesmo índice d) senão a direção fica errada.
    static int[] criarDeslocamentoColuna(int ordem) {
        if (ordem == 0) {
            return new int[]{0, 1, 0, -1};
        }
        return new int[]{-1, 0, 1, 0};
    }

    

    // Vê se a posição (i,j) existe na grade. Usamos antes de olhar um vizinho, porque
    // nas bordas falta vizinho e sem isso dava ArrayIndexOutOfBoundsException.
    static boolean dentro(int i, int j, int n, int m) {
        return i >= 0 && i < n && j >= 0 && j < m;
    }


    // Copia a matriz valor por valor. Precisa porque em Java fazer matriz2 = matriz1
    // só copia a referência, aí mexer em uma mexia na outra e a gente perdia a lâmina
    // inicial e a do ciclo 3.
    static int[][] copiar(int[][] matriz) {
        int[][] copia = new int[matriz.length][matriz[0].length];
        for (int i = 0; i < matriz.length; i++) {
            for (int j = 0; j < matriz[0].length; j++) {
                copia[i][j] = matriz[i][j];
            }
        }
        return copia;
    }


    // Mostra a matriz no console, uma linha da grade por linha de texto, com tab
    // entre os números. Criamos pra não ficar repetindo o for duplo toda hora que
    // precisa mostrar altitude ou lâmina.
    static void imprimir(int[][] matriz, String titulo) {
        System.out.println(titulo);
        for (int i = 0; i < matriz.length; i++) {
            String linha = "";
            for (int j = 0; j < matriz[0].length; j++) {
                if (j > 0) {
                    linha += "\t";
                }
                linha += matriz[i][j];
            }
            System.out.println(linha);
        }
    }


    // Soma tudo da matriz. Usamos na lâmina pra ver o total de água antes e depois de
    // cada ciclo. Como a água só muda de lugar, o total não pode mudar, então serve
    // pra gente conferir se a simulação tá certa.
    static int somar(int[][] matriz) {
        int total = 0;
        for (int i = 0; i < matriz.length; i++) {
            for (int j = 0; j < matriz[0].length; j++) {
                total += matriz[i][j];
            }
        }
        return total;
    }

    

    // Nível da água na célula = altitude + lâmina. Usamos o nível e não só a altitude
    // porque uma célula baixa mas cheia de água pode estar "mais alta" que uma vizinha
    // mais alta e seca.
    static int nivel(int[][] altitude, int[][] lamina, int i, int j) {
        return altitude[i][j] + lamina[i][j];
    }


    // Roda um ciclo da simulacão do jeito certo. Cada célula com água suficiente
    // (metade da lâmina maior que 0) procura o vizinho de menor nível, que tem que ser
    // menor que o dela, e manda essa metade pra lá. O importante é que lemos sempre a
    // matriz antiga e gravamos numa cópia (nova), aí todas as células andam ao mesmo
    // tempo e a ordem da varredura não muda o resultado. Como só transfere agua,
    // o total continua igual.
    static int[][] simularPasso(int[][] altitude, int[][] lamina,
                                int[] deslocamentoLinha, int[] deslocamentoColuna) {
        int n = altitude.length;
        int m = altitude[0].length;
        int[][] nova = copiar(lamina);

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                int t = lamina[i][j] / 2;
                if (t > 0) {
                    int melhor = nivel(altitude, lamina, i, j);
                    int escolhido = -1;
                    for (int d = 0; d < 4; d++) {
                        int vi = i + deslocamentoLinha[d];
                        int vj = j + deslocamentoColuna[d];
                        if (dentro(vi, vj, n, m)) {
                            int nv = nivel(altitude, lamina, vi, vj);
                            if (nv < melhor) {
                                melhor = nv;
                                escolhido = d;
                            }
                        }
                    }
                    if (escolhido != -1) {
                        int vi = i + deslocamentoLinha[escolhido];
                        int vj = j + deslocamentoColuna[escolhido];
                        nova[i][j] -= t;
                        nova[vi][vj] += t;
                    }
                }
            }
        }
        return nova;
    }


    // Versão "errada" de propósito, só pra comparar com a certa (D1 x D2). A regra é a
    // mesma, mas aqui mexemos direto na lâmina durante a varredura. Ai a célula que vem
    // depois já enxerga a água que acabou de chegar, e o resultado depende da ordem
    // (a água chega a andar mais de uma casa por ciclo). Mostra por que precisamos da
    // cópia no simularPasso.
    static int[][] simularPassoIngenuo(int[][] altitude, int[][] lamina,
                                       int[] deslocamentoLinha, int[] deslocamentoColuna) {
        int n = altitude.length;
        int m = altitude[0].length;

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                int t = lamina[i][j] / 2;
                if (t > 0) {
                    int melhor = nivel(altitude, lamina, i, j);
                    int escolhido = -1;
                    for (int d = 0; d < 4; d++) {
                        int vi = i + deslocamentoLinha[d];
                        int vj = j + deslocamentoColuna[d];
                        if (dentro(vi, vj, n, m)) {
                            int nv = nivel(altitude, lamina, vi, vj);
                            if (nv < melhor) {
                                melhor = nv;
                                escolhido = d;
                            }
                        }
                    }
                    if (escolhido != -1) {
                        int vi = i + deslocamentoLinha[escolhido];
                        int vj = j + deslocamentoColuna[escolhido];
                        lamina[i][j] -= t;
                        lamina[vi][vj] += t;
                    }
                }
            }
        }
        return lamina;
    }


    // Faz uma matriz de true/false dizendo quais células estão em alerta (lâmina >=
    // limiar). Separamos da lâmina pra poder contar, desenhar o mapa e achar as regiões
    // sem ficar comparando toda hora.
    static boolean[][] marcarAlerta(int[][] lamina, int limiarAlerta) {
        boolean[][] alerta = new boolean[lamina.length][lamina[0].length];
        for (int i = 0; i < lamina.length; i++) {
            for (int j = 0; j < lamina[0].length; j++) {
                alerta[i][j] = lamina[i][j] >= limiarAlerta;
            }
        }
        return alerta;
    }


    // Conta quantas células estão em alerta. É o número de células alagadas que sai
    // no relatório e no RESUMO.
    static int contarAlerta(boolean[][] alerta) {
        int contador = 0;
        for (int i = 0; i < alerta.length; i++) {
            for (int j = 0; j < alerta[0].length; j++) {
                if (alerta[i][j]) {
                    contador++;
                }
            }
        }
        return contador;
    }


    // Desenha o mapa de alerta: # pra alagado e . pra normal. Ficou bem mais facil de
    // enxergar do que só números, da pra ver se o alagamento fica junto perto do vale.
    static void imprimirMapaAlerta(boolean[][] alerta) {
        System.out.println("Mapa de alerta (# = alagado, . = normal):");
        for (int i = 0; i < alerta.length; i++) {
            String linha = "";
            for (int j = 0; j < alerta[0].length; j++) {
                if (j > 0) {
                    linha += "\t";
                }
                if (alerta[i][j]) {
                    linha += "#";
                } else {
                    linha += ".";
                }
            }
            System.out.println(linha);
        }
    }


    // Soma a água de cada linha e devolve um vetor com o total por linha. Precisa pra
    // achar a linha crítica, que deve ficar perto do meio por causa do relevo.
    static int[] calcularAguaPorLinha(int[][] lamina) {
        int[] agua = new int[lamina.length];
        for (int i = 0; i < lamina.length; i++) {
            for (int j = 0; j < lamina[0].length; j++) {
                agua[i] += lamina[i][j];
            }
        }
        return agua;
    }


    // Acha o índice da linha com mais água. Começa na linha 0 e só troca quando acha
    // uma maior de verdade, então se empatar fica a primeira linha.
    static int encontrarLinhaCritica(int[] aguaPorLinha) {
        int critica = 0;
        for (int i = 1; i < aguaPorLinha.length; i++) {
            if (aguaPorLinha[i] > aguaPorLinha[critica]) {
                critica = i;
            }
        }
        return critica;
    }


    // Conta quantas regiões alagadas tem (grupos de células em alerta coladas pelos
    // lados) e o tamanho da maior. Usamoss busca em profundidade com pilha feita de
    // vetor e uma matriz visitada pra não contar a mesma célula duas vezes. Toda vez
    // que acha uma célula em alerta ainda não visitada é região nova, e empilha os
    // vizinhos em alerta até acabar. Preferimos pilha no lugar de recursão pra não
    // arriscar estourar a pilha de chamadas. Retorna {quantidade, maior}.
    static int[] analisarRegioes(boolean[][] alerta, int[] deslocamentoLinha,
                                 int[] deslocamentoColuna) {
        int n = alerta.length;
        int m = alerta[0].length;
        boolean[][] visitada = new boolean[n][m];
        int[] pilhaI = new int[n * m];
        int[] pilhaJ = new int[n * m];
        int quantidade = 0;
        int maior = 0;

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                if (alerta[i][j] && !visitada[i][j]) {
                    quantidade++;
                    int tamanho = 0;
                    int topo = 0;
                    pilhaI[topo] = i;
                    pilhaJ[topo] = j;
                    topo++;
                    visitada[i][j] = true;
                    while (topo > 0) {
                        topo--;
                        int ci = pilhaI[topo];
                        int cj = pilhaJ[topo];
                        tamanho++;
                        for (int d = 0; d < 4; d++) {
                            int vi = ci + deslocamentoLinha[d];
                            int vj = cj + deslocamentoColuna[d];
                            if (dentro(vi, vj, n, m) && alerta[vi][vj] && !visitada[vi][vj]) {
                                visitada[vi][vj] = true;
                                pilhaI[topo] = vi;
                                pilhaJ[topo] = vj;
                                topo++;
                            }
                        }
                    }
                    if (tamanho > maior) {
                        maior = tamanho;
                    }
                }
            }
        }
        return new int[]{quantidade, maior};
    }


    // Programa principal: lê os RMs, calcula os parâmetros, gera terreno e chuva e roda
    // os K ciclos mostrando o total de água e os alertas novos de cada um. No fim
    // imprime o relatório (células em alerta, mapa, água por linha, linha crítica e
    // regiões). Por último compara a simulação certa com a ffalsa no ciclo 3 (D1 e D2)
    // e imprime o RESUMO da equipe.
    public static void main(String[] args) {
        Scanner leitor = new Scanner(System.in);



        System.out.println("Quantidade de integrantes (1 a 3):");
        int qtd = leitor.nextInt();
        int[] rms = new int[qtd];
        for (int i = 0; i < qtd; i++) {
            System.out.println("RM do integrante " + (i + 1) + ":");
            rms[i] = leitor.nextInt();
        }

        int rm = calcularRmEquipe(rms);
        int[] parametros = calcularParametros(rm);
        int n = parametros[0];
        int m = parametros[1];
        int k = parametros[2];
        int limiar = parametros[3];
        int ordem = parametros[4];

        System.out.println("RM da equipe: " + rm);
        System.out.println("Parametros: N=" + n + " M=" + m + " K=" + k
                + " L=" + limiar + " ordem=" + ordem);

        int[] deslocamentoLinha = criarDeslocamentoLinha(ordem);
        int[] deslocamentoColuna = criarDeslocamentoColuna(ordem);

        Random gerador = new Random(rm);
        int[][] altitude = gerarAltitude(gerador, n, m);
        int[][] laminaOriginal = gerarMatriz(gerador, n, m, 0, 10);
        int[][] lamina = copiar(laminaOriginal);

        imprimir(altitude, "Altitude (cm):");
        imprimir(lamina, "Lamina inicial (cm):");



        int totalInicio = somar(lamina);
        int[][] laminaApos3 = null;
        boolean[][] alertaAnterior = marcarAlerta(lamina, limiar);

        for (int ciclo = 1; ciclo <= k; ciclo++) {
            int antes = somar(lamina);
            lamina = simularPasso(altitude, lamina, deslocamentoLinha, deslocamentoColuna);
            int depois = somar(lamina);

            imprimir(lamina, "Lamina apos o ciclo " + ciclo + " (cm):");
            System.out.println("Total de agua antes do ciclo " + ciclo + ": " + antes
                    + " | depois: " + depois);

            boolean[][] alertaAtual = marcarAlerta(lamina, limiar);
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < m; j++) {
                    if (alertaAtual[i][j] && !alertaAnterior[i][j]) {
                        System.out.println("ALERTA: ciclo " + ciclo + ", celula ("
                                + i + "," + j + ") atingiu " + lamina[i][j] + " cm");
                    }
                }
            }
            alertaAnterior = alertaAtual;

            if (ciclo == 3) {
                laminaApos3 = copiar(lamina);
            }
        }

        int totalFim = somar(lamina);
        System.out.println("Total de agua no inicio: " + totalInicio);
        System.out.println("Total de agua no fim: " + totalFim);

        boolean[][] alerta = marcarAlerta(lamina, limiar);
        int totalAlerta = contarAlerta(alerta);
        System.out.println("Celulas em alerta: " + totalAlerta);




        imprimirMapaAlerta(alerta);

        int[] aguaPorLinha = calcularAguaPorLinha(lamina);
        System.out.println("Agua por linha (cm):");
        for (int i = 0; i < n; i++) {
            System.out.println("Linha " + i + ": " + aguaPorLinha[i]);
        }
        int critica = encontrarLinhaCritica(aguaPorLinha);
        System.out.println("Linha critica: " + critica + " (" + aguaPorLinha[critica] + " cm)");

        int[] regioes = analisarRegioes(alerta, deslocamentoLinha, deslocamentoColuna);
        System.out.println("Regioes alagadas: " + regioes[0]);
        System.out.println("Tamanho da maior regiao: " + regioes[1]);




        int a = rm % n;
        int b = (rm / 7) % m;
        int d1 = laminaApos3[a][b];

        int[][] laminaIngenua = copiar(laminaOriginal);
        for (int passo = 1; passo <= 3; passo++) {
            laminaIngenua = simularPassoIngenuo(altitude, laminaIngenua,
                    deslocamentoLinha, deslocamentoColuna);
        }
        int d2 = laminaIngenua[a][b];

        System.out.println("D1 e D2 calculados para a=" + a + " b=" + b);

        System.out.println("RESUMO RM=" + rm);
        System.out.println("ALERTA=" + totalAlerta);
        System.out.println("CRITICA=" + critica + " AGUA=" + aguaPorLinha[critica]);
        System.out.println("D1=" + d1 + " D2=" + d2);
    }
}