package simulador;

public class IO extends Evento {
    private int     tempoExecutado;
    private boolean finalizado;


    public IO(int tempoChegada, int tempoExecucao) {
        super(tempoChegada, tempoExecucao);
        tempoExecutado = 0;
        finalizado     = false;
    }


    public void execTick() {  // Executa um único tick
        if (tempoExecutado < tempoExecucao) {  
            ++tempoExecutado;
        
        } else if (!finalizado && tempoExecutado == tempoExecucao) {  // Marca como finalizado e gera interrupção
            finalizado = true;
            interrupcao();
        } 
    }


    private void interrupcao() {
        // TODO
    }


    public boolean ativo() {
        return finalizado;
    }


    public void setpBack(boolean emTratamento) {  // Volta 1 tick no tempo
        if (!emTratamento)                        // Se não estava executando no tick atual não precisa fazer nada
            return;

        --tempoExecutado;
        if (finalizado)
            finalizado = false;
    }
}
