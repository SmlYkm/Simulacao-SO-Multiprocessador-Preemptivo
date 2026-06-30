package simulador;

public class IO extends Evento {
    private int     tempoExecutado;
    private int     tempoExecucao;
    private boolean finalizado;


    public IO(int tempoChegada, int tempoExecucao) {
        super(tempoChegada);
        this.tempoExecucao  = tempoExecucao;
        this.tempoExecutado = 0;
        this.finalizado     = false;
    }


    public void execTick() {  // Executa um único tick
        if (tempoExecutado < tempoExecucao) {  
            ++tempoExecutado;
        
        } else if (!finalizado && tempoExecutado == tempoExecucao) {  // Marca como finalizado e gera interrupção
            finalizado = true;
            // interrupcao();
        } 
    }

    
    public int getTempoExecucao() {
        return tempoExecucao;
    }


    public int getTempoExecutado() {
        return tempoExecutado;
    }


    // private void interrupcao() {
    //     // 
    // }


    public boolean isFinalizado() {
        return finalizado;
    }


    public void stepBack() {  // Volta 1 tick no tempo
        if (tempoExecutado > 0)
            --tempoExecutado;
        if (finalizado)
            finalizado = false;
    }
}
