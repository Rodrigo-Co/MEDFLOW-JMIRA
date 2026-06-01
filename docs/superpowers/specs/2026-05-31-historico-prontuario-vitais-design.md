# Historico de Prontuario e Vitais Design

## Objetivo

Adicionar rastreabilidade para edicoes de prontuario, tomada de responsabilidade por prontuarios de outros medicos, e atualizacao de sinais vitais/medicacoes de pacientes vinculada a um prontuario justificativo.

## Regras Aprovadas

- O medico responsavel pode editar seu proprio prontuario.
- Se outro medico precisar alterar ou adicionar algo em um prontuario, ele deve primeiro assumir o prontuario.
- Assumir um prontuario troca o medico responsavel, registra historico no prontuario e gera log administrativo.
- Toda edicao de prontuario deve exibir historico com quem alterou, quando alterou e o que mudou.
- O medico pode atualizar pressao arterial, frequencia cardiaca e medicacoes de um paciente apenas selecionando um prontuario existente do mesmo paciente.
- O prontuario justificativo deve ser de tipo `consulta`, `exame` ou `cirurgia`.
- Atualizacoes clinicas devem gerar historico dedicado e log administrativo.

## Abordagem

O backend continua centralizando regras em `RecordService` e persistencia nos repositorios JDBC existentes. O historico de prontuario usa a tabela `record_edit_history`, mas passa a receber descricoes detalhadas das alteracoes. A tomada de responsabilidade e implementada como uma operacao explicita, separada da edicao.

Para sinais vitais e medicacoes, sera criado um fluxo medico que recebe `patientId`, `recordId` e os dados a atualizar. O backend valida o vinculo entre paciente e prontuario antes de gravar novos itens nas tabelas de historico e medicacoes. Um novo historico operacional (`patient_health_update_history`) registra medico, paciente, prontuario justificativo e resumo das mudancas.

## Interface

No portal medico, a tela de prontuarios passa a mostrar historico de edicoes no modal de visualizacao. Prontuarios de outro medico exibem a acao `Assumir prontuario`; depois da tomada de responsabilidade, o botao de edicao fica disponivel.

Na mesma area de prontuarios, o medico podera abrir uma acao de atualizacao clinica para o paciente do prontuario selecionado. A interface envia o `recordId` junto dos dados de pressao, frequencia cardiaca e medicacoes.

## Seguranca e Integridade

- Nenhuma edicao de prontuario de terceiro ocorre sem tomada explicita.
- Nenhuma atualizacao clinica ocorre sem prontuario justificativo.
- Todo fluxo sensivel cria `audit_log`.
- O dashboard do paciente continua consumindo o ultimo item dos historicos existentes, preservando o comportamento atual.

## Fora de Escopo

- Criar sistema de aprovacao admin para tomada de prontuario.
- Alterar autenticacao, JWT ou 2FA.
- Reestruturar toda a persistencia.
