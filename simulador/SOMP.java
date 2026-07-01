package simulador;

import java.util.ArrayList;

public class SOMP {
    private Escalonador escalonador;
    private Processador[] processadores;
    private int tempoAtual;
    private java.util.Map<Integer, Mutex> tabelaMutex = new java.util.HashMap<>();

    private ArrayList<Tarefa> listaTarefasGeral;

    public SOMP(Escalonador escalonador, int numProcessadores) {
        this.escalonador = escalonador;
        this.processadores = new Processador[numProcessadores];
        for (int i = 0; i < numProcessadores; i++) {
            this.processadores[i] = new Processador(i);
        }
        this.tempoAtual = 0;

        this.listaTarefasGeral = new ArrayList<>();
    }

    private class TratamentoIRQ {
        Tarefa tarefa;
        IO io;
        TratamentoIRQ(Tarefa t, IO i) { this.tarefa = t; this.io = i; }
    }
    private List<TratamentoIRQ> filaIRQ = new ArrayList<>();

    public void registrarIRQ(Tarefa t, IO io) {
        filaIRQ.add(new TratamentoIRQ(t, io)); // Hardware enfileira a IRQ
    }

    // Um novo método para receber as tarefas do LeitorConfig:
    public void adicionarTarefa(Tarefa t) {
        this.listaTarefasGeral.add(t);
    }

    // Método para o Main poder puxar a lista de tarefas e enviar para a Window:
    // public List<Tarefa> getListaTarefasGeral() {
    //     return listaTarefasGeral;
    // }

    public int getTotalNumTarefas() {
        return listaTarefasGeral.size();
    }

    public Tarefa getTarefaByIdx(int idx) {
        if (listaTarefasGeral != null)  // && id >= 0 && id < listaTarefasGeral.size())
            return listaTarefasGeral.get(idx);  // get ja tem checagem de idx
        return null;
    }

    public void executar() {  // Execução de 1 tick
        for (Tarefa tarefa : listaTarefasGeral) {      // Coloca tarefas que chegaram agora no escalonador
            if (tarefa.getTempoChegada() == tempoAtual)
                escalonador.adicionarTarefa(tarefa);
        }

        // Organiza a fila
        escalonador.prepararFila(processadores, quantum);  

        // Pega as tarefas da fila pela função obterproximatarefa
        List<Tarefa> topTarefas = new ArrayList<>();
        for (int i = 0; i < processadores.length; i++) {
            Tarefa proxima = escalonador.obterProximaTarefa();
            if (proxima != null) {
                topTarefas.add(proxima);
            }
        }

        // Distribui as tarefas nos processadores mantendo a afinidade com as tarefas
        for (Processador cpu : processadores) {
            Tarefa tarefaAtual = cpu.getTarefaAtual();
            if (tarefaAtual != null && topTarefas.contains(tarefaAtual)) {
                topTarefas.remove(tarefaAtual); // Continua no mesmo processador
            } else {
                cpu.setTarefaAtual(null); // Sai do processador
            }
        }
        //Atribui as tarefas restantes
        for (Processador cpu : processadores) { 
            if (cpu.idle() && !topTarefas.isEmpty()) {
                cpu.setTarefaAtual(topTarefas.remove(0));  
            }
        }

        // Grava o estado e executa o tick
        escalonador.executar(processadores, quantum);  // Escalonador executa seu algoritmo
        gravarHistorico();
        for (Processador cpu : processadores) {        // Executa 1 tick por processador
            cpu.executar();                       
        }

        for (Tarefa t : listaTarefasGeral) {
            if (t.getTempoChegada() <= tempoAtual && t.isBloqueada())
                t.executarIO(this);
        }

        ++tempoAtual;
    }

    private void gravarHistorico() {
        for (Tarefa tarefa : listaTarefasGeral) {
            if (tarefa.isFinalizada()) {                         // Terminou
                tarefa.registrarEstado(tempoAtual, Tarefa.Estado.Finalizado, -1, 0);
                continue;
            } else if (tarefa.isEsperandoMutex()) {              
                tarefa.registrarEstado(tempoAtual, Tarefa.Estado.EsperandoMutex, -1, 0);
            }
            if (tarefa.getTempoChegada() > tempoAtual) {  
                tarefa.registrarEstado(tempoAtual, Tarefa.Estado.NaoCriada, -1, 0);
                continue;
            
            } else if (tarefa.isFinalizada()) {                          
                tarefa.registrarEstado(tempoAtual, Tarefa.Estado.Finalizado, -1, 0);
                continue;
            
            } else if (tarefa.isSuspensa()) {                    
                tarefa.registrarEstado(tempoAtual, Tarefa.Estado.Suspenso, -1, 0);
                continue;
            
            } else if (tarefa.isBloqueada()) {                   
                tarefa.registrarEstado(tempoAtual, Tarefa.Estado.Bloqueado, -1, 0);
                continue;
            }

            int cpuId = -1;  // Descobre se a tarefa está em alguma cpu
            for (Processador proc : processadores) {
                if (
                    proc.getTarefaAtual()         != null && 
                    proc.getTarefaAtual().getId() == tarefa.getId()
                ) {
                    cpuId = proc.getId();
                    break;
                }
            }

            if (cpuId != -1) {  // Ta rodando em algum lugar
                tarefa.registrarEstado(tempoAtual, Tarefa.Estado.Executando, cpuId, processadores[cpuId].getTicksNoQuantum());
            } else {            // Não ta rodando, mas já chegou e não terminou -> está esperando
                tarefa.registrarEstado(tempoAtual, Tarefa.Estado.Esperando, -1, 0);
            }
        }
    }
    
    public int getTempoAtual() {  // Retorna o tempo real da simulação
        return tempoAtual; 
    }
    
    public boolean isFinalizado() {  // Verifica se todas as tarefas já terminaram
        for (Tarefa t : listaTarefasGeral) {
            if (!t.isFinalizada()) {
                return false;       // Se encontrar uma que não terminou, retorna falso
            }
        }
        return true; // Todas terminaram
    }

    public Processador[] getProcessadores() {
        return processadores;
    }

    public void stepBack() {
        if (tempoAtual <= 0) return;
        --tempoAtual; //Volta 1 tick

        // Restaura o valor das tarefas
        for (Tarefa t : listaTarefasGeral) {
            Tarefa.TickSnapshot reg = t.getRegistroNoTempo(tempoAtual);
            
            if (reg.estado == Tarefa.Estado.Executando) {
                t.setTempoRestante(t.getTempoRestante() + 1); // Devolve o tempo que tinha gasto
                t.setFinalizada(false); // Ressuscita a tarefa caso ela tenha morrido neste tick
            }
            
            // Apaga a coluna a frente
            t.apagarRegistroNoTempo(tempoAtual);
        }

        // Limpa os processadores
        // O método executar vai reconstruir a fila a partir da lista geral
        for (Processador cpu : processadores) {
            cpu.apagarRegistroOciosidade(tempoAtual); //desfaz o registro de ociosidade naquele tick
            cpu.setTarefaAtual(null);
            cpu.resetTicksNoQuantum(); 
        }
    }
}