-- Dados de teste para validar:
-- 1. medico assumindo prontuario de outro responsavel;
-- 2. medico editando prontuario assumido;
-- 3. medico atualizando frequencia cardiaca atual usando prontuario justificativo;
-- 4. admin vendo logs com o que foi editado.
--
-- Execute este arquivo no Supabase SQL Editor do projeto atual.

INSERT INTO medical_records (id, patient_id, patient_name, doctor_id, doctor_name, record_date, type, raw_notes, formatted_notes, diagnosis) VALUES
('rtest-assumir-maria','p1','Maria da Silva','d2','Dra. Ana Beatriz Lima','2026-05-27','consulta',
 'teste de continuidade: paciente refere palpitacoes leves e oscilacao de pressao; revisar conduta apos assumir prontuario',
 'Paciente refere palpitacoes leves e oscilacao pressorica. Registro criado para teste de continuidade assistencial; o medico deve assumir o prontuario antes de editar a conduta.',
 'Teste - assumir e editar prontuario de Maria'),
('rtest-assumir-joao','p2','Joao Carlos Pereira','d3','Dr. Carlos Eduardo Silva','2026-05-28','consulta',
 'teste para outro medico assumir; paciente relata cansaco aos esforcos, sem dor toracica no momento',
 'Paciente relata cansaco aos esforcos, sem dor toracica no momento. Registro criado para teste de assuncao e edicao de prontuario por outro medico.',
 'Teste - assumir e editar prontuario de Joao'),
('rtest-fc-joao','p2','Joao Carlos Pereira','d3','Dr. Carlos Eduardo Silva','2026-05-29','exame',
 'teste de atualizacao de frequencia cardiaca atual apos exame; ecg com ritmo sinusal, frequencia basal elevada',
 'Exame registrado para teste de atualizacao de frequencia cardiaca atual. ECG com ritmo sinusal e frequencia basal elevada, sem sinais de instabilidade no registro.',
 'Teste - atualizar frequencia cardiaca atual'),
('rtest-assumir-ana','p3','Ana Paula Rodrigues','d1','Dr. Ricardo Mendes','2026-05-30','cirurgia',
 'teste para cirurgiao assumir prontuario; acompanhamento pos procedimento, sem intercorrencias agudas',
 'Acompanhamento pos-procedimento sem intercorrencias agudas. Registro criado para teste de assuncao por medico diferente do responsavel atual.',
 'Teste - assumir prontuario cirurgico')
ON CONFLICT (id) DO NOTHING;

INSERT INTO record_edit_history (id, record_id, edited_by, edited_by_name, edit_timestamp, changes) VALUES
('ehtest-maria-1','rtest-assumir-maria','d2','Dra. Ana Beatriz Lima','2026-05-27T14:20:00Z','Diagnostico alterado de ''Teste inicial'' para ''Teste - assumir e editar prontuario de Maria''; Notas formatadas alteradas.'),
('ehtest-joao-1','rtest-assumir-joao','d3','Dr. Carlos Eduardo Silva','2026-05-28T15:10:00Z','Notas brutas alteradas; Conduta revisada para teste.')
ON CONFLICT (id) DO NOTHING;

INSERT INTO audit_log (id, action, severity, details, user_id, user_name, user_role, log_timestamp) VALUES
('altest-edit-maria','RECORD_EDIT','warning','Prontuario de Maria da Silva editado: Diagnostico alterado de ''Teste inicial'' para ''Teste - assumir e editar prontuario de Maria''; Notas formatadas alteradas.','d2','Dra. Ana Beatriz Lima','doctor','2026-05-27T14:20:00Z'),
('altest-edit-joao','RECORD_EDIT','warning','Prontuario de Joao Carlos Pereira editado: Notas brutas alteradas; Conduta revisada para teste.','d3','Dr. Carlos Eduardo Silva','doctor','2026-05-28T15:10:00Z')
ON CONFLICT (id) DO NOTHING;
