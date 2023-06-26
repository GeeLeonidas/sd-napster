## Problemas não previstos pelo plano do projeto
- Caso mais de um *peer* tenha o mesmo IP, o servidor não consegue detectar quem enviou a requisição `SEARCH` apenas com os parâmetros pedidos
- Não existe forma de verificar a resiliência do servidor TCP e cancelar seu registro em caso de falha

## Ambiente utilizado
- JDK 1.8 (Eclipse Temurin)
- Linux (kernel 6.3.9-5-default)

## Instruções
- Executando o comando `gradlew compileJava`, os arquivos .class serão gerados na pasta `build/classes/java/main`
- Para iniciar o servidor, execute `CLASSPATH="build/classes/java/main" java br.dev.gee.sdnapster.Servidor`
  - Inicialize o servidor inserindo o endereço e a porta em que deseja disponibilizá-lo
- Para iniciar o *peer*, execute `CLASSPATH="build/classes/java/main" java br.dev.gee.sdnapster.Peer`
  - Insira o endereço e a porta em que o servidor está rodando
  - Escolha uma das quatro opções disponíveis (`JOIN`, `SEARCH`, `DOWNLOAD`, `EXIT`)
    - `JOIN` só pode ser executada uma vez, inserindo os dados que inicializarão tanto o estado do *peer* quanto seu servidor TCP (i.e. endereço, arquivos disponibilizados)
    - `SEARCH` só pode ser executada após `JOIN`, inserindo o nome do arquivo que se quer achar na rede
    - `DOWNLOAD` só pode ser executada após `SEARCH`, inserindo de qual *peer* você quer baixar o arquivo pesquisado anteriormente
    - `EXIT` termina o programa com código 0
