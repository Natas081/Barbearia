# BarbeariaBD — Sistema de Gestão de Barbearia

**Projeto Final — Etapa 06**  
**Disciplina:** Banco de Dados  
**Instituição:** CESAR School

## Integrantes

- Natan Luis
- Felipe Lemos
- Felipe Menezes
- Diego Gomes
- Ranilton Junior

---

## 1. Apresentação do Projeto

O BarbeariaBD é uma aplicação desktop desenvolvida em Java, integrada a um banco de dados MySQL, com o objetivo de gerenciar as principais operações de uma barbearia.

O sistema contempla o cadastro de clientes, profissionais e serviços, o gerenciamento de agendamentos e atendimentos, a execução de consultas SQL e a visualização de indicadores estatísticos por meio de um dashboard interativo.

O projeto foi desenvolvido progressivamente ao longo de seis etapas, abrangendo modelagem de dados, implementação do banco relacional, desenvolvimento da interface, consultas avançadas, índices, views, funções, procedimentos armazenados e triggers.

A versão final integra esses recursos em uma única aplicação.

## 2. Tecnologias Utilizadas

- **Java 21:** linguagem principal da aplicação.
- **Java Swing:** construção da interface gráfica.
- **FlatLaf:** personalização visual da interface.
- **MySQL 8:** sistema gerenciador de banco de dados.
- **JDBC:** comunicação entre Java e MySQL, utilizando comandos SQL explícitos.
- **Maven:** gerenciamento de dependências e execução do projeto.
- **JFreeChart:** construção dos gráficos estatísticos.
- **JUnit 5:** testes automatizados.
- **DBeaver:** administração, execução e validação dos scripts SQL.
- **IntelliJ IDEA:** ambiente de desenvolvimento.

## 3. Funcionalidades da Aplicação

### 3.1. Gerenciamento de Cadastros

A aplicação disponibiliza operações de cadastro, consulta, atualização e exclusão de registros, incluindo:

- **Clientes:** gerenciamento dos dados cadastrais.
- **Profissionais:** cadastro de profissionais e suas especializações como barbeiro ou gerente.
- **Serviços:** gerenciamento do catálogo, preços e informações dos serviços oferecidos.
- **Agendamentos:** registro de reservas, seleção de clientes e barbeiros, definição de horários e atualização de status.

O sistema também permite gerenciar atendimentos e os serviços realizados em cada atendimento.

As operações são integradas ao banco de dados, respeitando as restrições de integridade referencial e as regras de negócio estabelecidas.

### 3.2. Consultas e Views

A aplicação disponibiliza consultas SQL desenvolvidas nas etapas anteriores, incluindo:

- Clientes e seus agendamentos.
- Agendamentos por barbeiro.
- Valores dos atendimentos.
- Serviços mais realizados.
- Clientes sem agendamentos.
- Clientes e profissionais, utilizando simulação de FULL OUTER JOIN.
- Serviços com preços acima da média.
- Barbeiros com quantidade de agendamentos acima da média.
- View de agenda detalhada.
- View de faturamento dos atendimentos.

Os resultados podem ser visualizados diretamente na interface, com recursos de pesquisa e filtros quando aplicáveis.

### 3.3. Dashboard Estatístico

O dashboard utiliza dados provenientes do banco MySQL e apresenta indicadores de desempenho da barbearia.

**Indicadores disponíveis:**

- Total de clientes.
- Total de profissionais.
- Quantidade de agendamentos.
- Atendimentos finalizados.
- Faturamento.
- Ticket médio.
- Quantidade de serviços executados.
- Percentual de agendamentos concluídos.

**Gráficos disponíveis:**

1. Faturamento ao longo do tempo.
2. Evolução do ticket médio.
3. Distribuição dos status dos agendamentos.
4. Quantidade de agendamentos por barbeiro.
5. Frequência dos serviços realizados.
6. Receita por serviço.
7. Histograma dos valores dos atendimentos.
8. Distribuição das classificações dos atendimentos.
9. Dispersão entre duração e valor dos atendimentos.

O dashboard também apresenta medidas estatísticas, incluindo média, mediana, moda, variância, desvio padrão, valores mínimos e máximos.

A variância utilizada é populacional, calculada com divisor N.

Os filtros permitem analisar períodos, barbeiros, serviços, status e diferentes agrupamentos temporais.

Os dados apresentados são obtidos diretamente do banco de dados, não sendo utilizados valores estáticos para simular os indicadores.

---

## 4. Etapa 04 — Consultas Avançadas, Views e Índices

### 4.1. Consultas Avançadas

Foram desenvolvidas quatro consultas avançadas, contemplando anti join, FULL OUTER JOIN simulado e subconsultas.

**Clientes sem agendamentos — Anti Join**

Identifica clientes que ainda não possuem agendamentos cadastrados.

**Justificativa semântica:** permite identificar clientes sem histórico de reservas, contribuindo para análises de utilização do sistema e possíveis ações de relacionamento com clientes.

**Clientes e profissionais — FULL OUTER JOIN**

Relaciona os cadastros de clientes e profissionais por CPF, permitindo identificar pessoas presentes em apenas um dos cadastros ou em ambos.

**Justificativa semântica:** possibilita analisar a sobreposição entre os cadastros e identificar pessoas que possuem diferentes vínculos com a barbearia.

Como o MySQL não oferece suporte nativo ao FULL OUTER JOIN, a operação foi implementada por meio de uma combinação de consultas SQL.

A ausência de CPFs correspondentes não invalida a consulta, pois a identificação de registros exclusivos também faz parte de sua finalidade.

**Serviços acima do preço médio — Subconsulta**

Seleciona serviços cujo preço é superior à média dos preços cadastrados.

**Justificativa semântica:** permite identificar serviços posicionados acima da média do catálogo, auxiliando na análise da política de preços.

**Barbeiros acima da média de agendamentos — Subconsulta**

Identifica barbeiros cuja quantidade de agendamentos supera a média considerada pela consulta.

**Justificativa semântica:** permite comparar a demanda entre profissionais e identificar aqueles com maior volume de agendamentos.

### 4.2. Views

**View `vw_agenda_detalhada`**

Consolida informações relacionadas aos agendamentos, reunindo dados relevantes para sua visualização e acompanhamento.

**Justificativa semântica:** facilita o acompanhamento operacional da agenda, reduzindo a necessidade de repetir consultas com múltiplas junções entre tabelas relacionadas.

**View `vw_faturamento_atendimento`**

Consolida informações financeiras dos atendimentos, permitindo analisar valores associados aos serviços executados.

**Justificativa semântica:** centraliza informações necessárias à análise do faturamento e facilita a construção de relatórios e indicadores financeiros.

As views permitem reutilizar consultas mais elaboradas e organizar o acesso aos dados sem duplicar informações armazenadas.

### 4.3. Índices

Na Etapa 04, foram definidos dois índices adicionais no script `barbearia.sql`, com o objetivo de otimizar operações de consulta relacionadas aos agendamentos e aos serviços realizados.

**Índice 1 — `idx_agendamento_barbeiro_status`**

```sql
CREATE INDEX idx_agendamento_barbeiro_status
ON agendamento (cpf_barbeiro, status);
```

**Justificativa semântica:** esse índice composto foi projetado para auxiliar consultas que localizam agendamentos de determinado barbeiro e filtram os resultados pelo status da reserva.

A primeira coluna permite localizar os agendamentos de um profissional, enquanto a segunda pode auxiliar na seleção por situação do agendamento.

**Índice 2 — `idx_item_atendimento_servico`**

```sql
CREATE INDEX idx_item_atendimento_servico
ON item_atendimento (id_servico);
```

**Justificativa semântica:** esse índice foi definido para auxiliar consultas que relacionam os serviços cadastrados aos itens dos atendimentos, como a análise de frequência de serviços e de faturamento por serviço.

**Observação técnica:** o MySQL/InnoDB também mantém índices necessários às restrições de chave estrangeira. Por isso, é necessário verificar se os índices adicionais não são redundantes com estruturas já existentes.

Os comandos acima fazem parte do script SQL do projeto. A existência dos índices no banco utilizado pela aplicação pode ser verificada com `SHOW INDEX`, enquanto sua utilização pelo otimizador pode ser analisada com `EXPLAIN`.

A definição dos índices representa uma proposta de otimização; ganhos efetivos de desempenho dependem das consultas executadas, da distribuição dos dados e do plano de execução escolhido pelo MySQL.

---

## 5. Etapa 05 — Funções, Procedimentos e Triggers

### 5.1. Funções SQL

**Função `fn_total_atendimento`**

Calcula o valor total de um atendimento a partir da soma dos preços praticados nos serviços registrados.

**Justificativa semântica:** centraliza o cálculo do valor financeiro de um atendimento, evitando que diferentes consultas ou funcionalidades utilizem regras distintas para calcular o total.

O cálculo considera os preços efetivamente praticados no atendimento, preservando os valores históricos.

**Função `fn_classificacao_atendimento`**

Classifica os atendimentos de acordo com o valor total calculado.

As classificações utilizadas são:

- Sem serviços: atendimento sem serviços lançados.
- Básico: valor inferior a R$ 100.
- Intermediário: valor de R$ 100 até menos de R$ 200.
- Premium: valor igual ou superior a R$ 200.

**Justificativa semântica:** permite segmentar os atendimentos por faixa de valor, contribuindo para análises comerciais e estatísticas.

A função utiliza estruturas condicionais para determinar a classificação correspondente.

### 5.2. Procedimentos Armazenados

**Procedure `sp_atualizar_preco_servico`**

Atualiza o preço de um serviço cadastrado, realizando as validações previstas na implementação.

**Justificativa semântica:** centraliza a atualização dos preços dos serviços, permitindo que a aplicação execute essa operação de maneira padronizada.

Sua utilização também permite que alterações de preços sejam registradas pelo trigger de auditoria.

**Procedure `sp_gerar_fechamento`**

Realiza o processamento de atendimentos finalizados para organizar seus valores financeiros em lotes de fechamento.

A procedure utiliza um cursor para percorrer os atendimentos em sequência, acumulando os valores conforme o limite informado.

**Justificativa semântica:** o processamento exige controle sequencial do acumulado de cada lote. A inclusão de um atendimento depende do total já acumulado nos atendimentos anteriores, justificando o uso de cursor em vez de uma simples atualização de registros.

Essa funcionalidade permite organizar os valores financeiros em agrupamentos definidos por um limite monetário.

### 5.3. Triggers

**Trigger `trg_log_preco_servico`**

Registra alterações realizadas nos preços dos serviços na tabela `log_preco_servico`.

O histórico contempla informações como preço anterior, preço novo, data da alteração e usuário do banco.

**Justificativa semântica:** permite auditar modificações de preços, oferecendo rastreabilidade e contribuindo para o controle administrativo da barbearia.

O registro automático reduz a dependência de operações manuais de auditoria.

**Trigger `trg_validar_conclusao_agendamento`**

Impede a conclusão de um agendamento quando não existe um atendimento finalizado e com pelo menos um serviço registrado.

**Justificativa semântica:** garante a consistência entre a situação do agendamento e a execução efetiva dos serviços.

Essa validação impede que uma reserva seja marcada como concluída sem atender às condições estabelecidas pelo sistema.

### 5.4. Integração com a Interface

As funções, procedures e triggers da Etapa 05 estão integradas à aplicação.

A interface disponibiliza recursos para:

- Consultar o valor e a classificação de atendimentos.
- Atualizar preços de serviços por meio de procedure.
- Visualizar o histórico de alterações de preços.
- Executar o fechamento financeiro por lotes.
- Verificar as regras de conclusão de agendamentos.

Os triggers são executados automaticamente pelo MySQL quando ocorrem as operações correspondentes.

---

## 6. Evolução do Modelo de Dados

O modelo de dados foi desenvolvido inicialmente na Etapa 01 e passou por ajustes durante a evolução do projeto.

Essas alterações foram necessárias para atender aos requisitos funcionais e incorporar as funcionalidades das etapas posteriores.

Entre as principais mudanças realizadas estão:

- Especialização dos profissionais em barbeiros e gerentes.
- Ajustes nos relacionamentos entre profissionais, agendamentos e atendimentos.
- Inclusão do vínculo do barbeiro responsável no agendamento.
- Inclusão do campo `horario_marcado`, permitindo registrar a data e o horário das reservas.
- Estruturação dos contatos telefônicos em tabelas específicas.
- Inclusão da tabela `log_preco_servico`, utilizada para auditoria de alterações de preços.
- Inclusão da tabela `fechamento_atendimento`, utilizada pelo procedimento de fechamento financeiro.
- Ajustes em chaves, relacionamentos e restrições de integridade.

Os modelos iniciais representam a estrutura concebida nas primeiras etapas, enquanto o esquema SQL final contempla as alterações implementadas ao longo do desenvolvimento.

Para verificar a estrutura efetivamente utilizada pela aplicação, devem ser considerados os scripts SQL finais e suas migrações.

---

## 7. Instalação e Configuração

### 7.1. Requisitos

Para executar a aplicação, é necessário:

- JDK 21.
- MySQL 8.0.19 ou superior da série 8.
- Ambiente gráfico compatível com Java Swing.
- Acesso ao Maven Wrapper incluído no projeto.
- Permissões adequadas para criação e utilização dos objetos SQL.

### 7.2. Configuração do Banco de Dados

Em uma instalação nova, execute os scripts SQL na seguinte ordem:

```sql
SOURCE SQL/barbearia.sql;
SOURCE SQL/entrega5.sql;
SOURCE SQL/etapa06.sql;
```

O arquivo `consultas.sql` contém consultas utilizadas para verificação independente.

**Atenção:** o script inicial contém comandos de exclusão de tabelas e deve ser executado apenas em uma instalação nova ou em uma base descartável.

Em uma base existente, recomenda-se realizar backup antes de aplicar qualquer alteração.

O script `etapa06.sql` inclui a migração referente ao horário marcado dos agendamentos.

Essa migração deve ser aplicada apenas quando necessária, evitando a recriação indevida de estruturas já existentes.

### 7.3. Credenciais do MySQL

Copie o arquivo:

`barbearia.local.example.properties`

E crie, na raiz do projeto:

`barbearia.local.properties`

Configure a URL JDBC, o usuário e a senha do banco.

Exemplo:

```properties
db.url=jdbc:mysql://127.0.0.1:3306/barbearia
db.user=SEU_USUARIO
db.password=SUA_SENHA
```

O arquivo com credenciais reais deve permanecer local e não deve ser incluído no repositório ou no ZIP de entrega.

Também é possível configurar a conexão por meio das variáveis de ambiente:

- `BARBEARIA_DB_URL`
- `BARBEARIA_DB_USER`
- `BARBEARIA_DB_PASSWORD`

As variáveis de ambiente têm precedência sobre os valores do arquivo local.

### 7.4. Execução pelo Terminal

Na raiz do projeto, execute:

```powershell
.\mvnw.cmd clean verify
.\mvnw.cmd exec:java
```

O primeiro comando compila o projeto e executa os testes configurados.

O segundo inicia a aplicação.

### 7.5. Execução pelo IntelliJ IDEA

1. Abra o projeto pelo arquivo `pom.xml`.
2. Configure o SDK Java 21.
3. Aguarde a sincronização das dependências Maven.
4. Crie uma configuração de execução do tipo Application.
5. Utilize a classe principal:

`com.natan.barbeariabd.BarbeariaBdApplication`

6. Configure o diretório de trabalho para a pasta raiz do projeto, onde está o arquivo `barbearia.local.properties`.
7. Execute a aplicação.

É importante que o diretório de trabalho esteja correto para que as configurações locais sejam carregadas.

---

## 8. Demonstração das Funcionalidades

Para demonstrar a integração entre a aplicação Java e os objetos SQL, pode-se seguir o roteiro abaixo.

**Teste 1 — Validação de agendamento**

Criar um agendamento e tentar concluí-lo sem atendimento finalizado.

Resultado esperado: o trigger impede a conclusão.

**Teste 2 — Registro de atendimento**

Registrar um atendimento, informar os horários e adicionar pelo menos um serviço.

Após finalizar o atendimento, tentar novamente concluir o agendamento.

Resultado esperado: a conclusão é permitida quando as condições são atendidas.

**Teste 3 — Funções SQL**

Na tela de operações do banco, selecionar um atendimento e executar as funções SQL.

Resultado esperado: exibição do valor total e da classificação correspondente.

**Teste 4 — Auditoria de preços**

Alterar o preço de um serviço por meio da funcionalidade de reajuste.

Resultado esperado: atualização do preço e criação de um registro na tabela de histórico.

**Teste 5 — Fechamento por lotes**

Executar a procedure de fechamento financeiro, informando um limite monetário positivo.

Resultado esperado: organização dos atendimentos finalizados em lotes, respeitando as regras da procedure.

**Teste 6 — Dashboard**

Aplicar filtros de período, profissional e serviço.

Resultado esperado: atualização dos indicadores e gráficos conforme os registros selecionados no banco de dados.

---

## 9. Testes e Validação

O projeto utiliza JUnit 5 para testes automatizados.

A execução padrão com `clean verify` realiza os testes unitários configurados.

O projeto também possui uma suíte de integração destinada à validação das operações com MySQL e da interface gráfica.

Os testes de integração devem ser executados preferencialmente em um banco descartável, evitando alterações nos dados utilizados pela aplicação principal.

A validação funcional deve contemplar:

- Operações CRUD.
- Restrições de integridade.
- Consultas e views.
- Funções e procedures.
- Execução e efeitos dos triggers.
- Indicadores estatísticos.
- Gráficos e filtros.
- Comunicação entre Java e MySQL.

A presença dos testes no projeto não substitui a verificação prática das funcionalidades durante a execução.

---

## 10. Organização do Projeto

A estrutura principal está organizada da seguinte maneira:

```text
BarbeariaBD/
├── SQL/
│   ├── barbearia.sql
│   ├── consultas.sql
│   ├── entrega5.sql
│   └── etapa06.sql
├── Modelos/
├── src/
│   ├── main/
│   │   └── java/
│   │       └── com/natan/barbeariabd/
│   │           ├── dao/
│   │           ├── database/
│   │           ├── model/
│   │           ├── service/
│   │           └── view/
│   └── test/
│       └── java/
├── barbearia.local.example.properties
├── pom.xml
└── README.md
```

Os arquivos SQL concentram a estrutura e as operações do banco de dados.

As classes Java estão organizadas conforme suas responsabilidades, separando acesso aos dados, modelos, serviços e componentes de interface.

---

## 11. Considerações Finais

O BarbeariaBD consolida os conteúdos desenvolvidos ao longo da disciplina de Banco de Dados, desde a modelagem inicial até a implementação de uma aplicação integrada.

O projeto utiliza recursos do MySQL, como consultas avançadas, views, índices, funções, procedimentos armazenados e triggers, associados a uma interface Java funcional.

A integração entre essas tecnologias permite gerenciar operações da barbearia, aplicar regras de negócio diretamente no banco de dados e produzir análises estatísticas a partir dos registros armazenados.

O desenvolvimento demonstra a aplicação prática dos conceitos de modelagem relacional, integridade de dados, programação SQL, integração JDBC e visualização de informações.