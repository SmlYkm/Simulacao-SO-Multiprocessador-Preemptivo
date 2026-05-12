package simulador;

public class SimulationController {
    private SOMP model;
    private Window view;

    public SimulationController(SOMP model, Window view) {
        this.model = model;
        this.view  = view;
        
        // Passa as tarefas do Model para a View desenhar
        for (int i = 0; i < model.getTotalNumTarefas(); ++i) {
            view.addTask(model.getTarefaByIdx(i));
        }
    }

    // A View chama este método quando o botão "Próximo Passo" for clicado
    public void stepForward(int currentViewTime) {
        if (currentViewTime < model.getTempoAtual()) {
            // Apenas avança o tempo visual (A interface estava retrocedida)
            view.setCurrentTime(currentViewTime + 1);
        } else {
            if (!model.isFinalizado()) {
                model.executar(); // Avança a simulação real
                view.setCurrentTime(model.getTempoAtual());
            } else {
                view.showError("A simulação já terminou!");
            }
        }
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
}