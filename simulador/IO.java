package simulador;

public class IO extends Evento {
    private int     tempoExecutado;
    private int     tempoExecucao;
    private boolean finalizado;
    private boolean irqTratada; 

    public IO(int tempoChegada, int tempoExecucao) {
        super(tempoChegada);
        this.tempoExecucao  = tempoExecucao;
        this.tempoExecutado = 0;
        this.finalizado     = false;
        this.irqTratada     = false; 
    }


    public void execTick(Tarefa tarefaDona, SOMP so) {   // Agora recebe a Tarefa dona e o SO para enviar a interrupção 
        if (tempoExecutado < tempoExecucao) {  
            ++tempoExecutado;
        } 
        
        if (tempoExecutado == tempoExecucao && !finalizado) {  
            finalizado = true;
            interrupcao(tarefaDona, so); // Gera IRQ
        } 
    }

    
    public int getTempoExecucao() {
        return tempoExecucao;
    }


    public int getTempoExecutado() {
        return tempoExecutado;
    }


    private void interrupcao(Tarefa t, SOMP so) {
        so.registrarIRQ(t, this); // Envia o sinal elétrico (IRQ) para o SO
    }


    public boolean isIrqTratada() { 
        return irqTratada; 
    }
    
    
    public void setIrqTratada(boolean irqTratada) { 
        this.irqTratada = irqTratada; 
    }


    public boolean isFinalizado() {
        return finalizado;
    }


    public void stepBack() {  
        if (tempoExecutado > 0)
            --tempoExecutado;

        if (finalizado)
            finalizado = false;

        if (irqTratada)
            irqTratada = false;
    }
}
