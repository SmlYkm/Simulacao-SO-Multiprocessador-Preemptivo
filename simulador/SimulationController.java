package simulador;

public class SimulationController {
    private SOMP   model;
    private Window view;

    public SimulationController(SOMP model, Window view) {
        this.model = model;
        this.view  = view;
        
        for (int i = 0; i < model.getTotalNumTarefas(); ++i) {
            view.addTask(model.getTarefaByIdx(i));  // Tarefa Model para a View desenhar
        }
    }
    
    public Processador[] getProcessadores() {
        return model.getProcessadores();
    }

    public void stepForward(int currentViewTime) {
        if (model.isFinalizado()) {
            view.showError("A simulação já terminou!");
            return;
        }

        model.executar(); // Avança a simulação real
        view.setCurrentTime(model.getTempoAtual());
    }

    public void stepBack() {  // Step back de 1 tick
        if (model.getTempoAtual() <= 0) {
            view.showError("A simulação já está no início!");
            return;
        }

        view.decCurrentTime();
        model.stepBack();
    }

    // A View chama este método quando o botão "Execução Completa" for clicado
    public void runAll() {
        while (!model.isFinalizado()) {
            model.executar();
        }
        view.setCurrentTime(model.getTempoAtual());
    }

    // O menu da View chama isso quando o usuário altera a prioridade
    public void changeTaskPriority(int taskIndex, int newPriority) {
        Tarefa t = model.getTarefaByIdx(taskIndex);
        t.setPrioridade(newPriority);
        // Opcional: registrar no terminal a alteração
        System.out.println("Prioridade da T" + t.getId() + " alterada para " + newPriority);
    }
    
    // O menu da View chama isso quando o usuário suspende/retoma
    public void toggleTaskSuspension(int taskIndex) {
        Tarefa t = model.getTarefaByIdx(taskIndex);
        t.suspender(!t.isSuspensa());
    }

    public int getTotalNumTarefas() {
        return model.getTotalNumTarefas();
    }
}