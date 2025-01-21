package org.operaton.rewrite.tests;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class TryCatchExtractCallArgsToVariableTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new TryCatchExtractCallArgsToVariable());
    }

    @Test
    void rewriteTryCatchToAssertThatThrownBy_methodArguments() {
        rewriteRun(spec -> spec.cycles(4).expectedCyclesThatMakeChanges(1), java("""
            package org.operaton.rewrite.tests;
          
            import org.operaton.bpm.engine.AuthorizationException;
            import org.operaton.bpm.engine.TaskService;
            import org.operaton.bpm.engine.task.Comment;
            import org.operaton.bpm.engine.test.util.ProcessEngineTestRule;
            import org.operaton.bpm.engine.test.util.ProvidedProcessEngineRule;
          
            import org.junit.Test;
          
            import static org.junit.Assert.fail;
          
            public class Sample {
                private static final String TASK_ID = "taskId";
                protected ProvidedProcessEngineRule engineRule = new ProvidedProcessEngineRule();
                protected ProcessEngineTestRule testRule = new ProcessEngineTestRule(engineRule);
                TaskService taskService;
          
                @Test
                public void testDeleteTaskCommentWithoutAuthorization() {
                    // given
                    createTask(TASK_ID);
                    Comment createdComment = createComment(TASK_ID, null, "aComment");
          
                    try {
                        // when
                        taskService.deleteTaskComment(TASK_ID, createdComment.getId());
                        fail("Exception expected: It should not be possible to delete a comment.");
                    } catch (AuthorizationException e) {
                        // then
                        testRule.assertTextPresent(
                          "The user with id 'test' does not have one of the following permissions: 'TASK_WORK' permission on resource 'myTask' of type 'Task' or 'UPDATE' permission on resource 'myTask' of type 'Task'",
                          e.getMessage());
                    }
          
                    // triggers a db clean up
                    deleteTask(TASK_ID, true);
                }
          
                private void deleteTask(String taskId, boolean b) {
          
                }
          
                private Comment createComment(String taskId, Object o, String aComment) {
                    return null;
                }
          
                private void createTask(String taskId) {
          
                }
          
            }
          """, """
          package org.operaton.rewrite.tests;
          
          import org.operaton.bpm.engine.AuthorizationException;
          import org.operaton.bpm.engine.TaskService;
          import org.operaton.bpm.engine.task.Comment;
          import org.operaton.bpm.engine.test.util.ProcessEngineTestRule;
          import org.operaton.bpm.engine.test.util.ProvidedProcessEngineRule;
          
          import org.junit.Test;
          
          import static org.junit.Assert.fail;
          
          public class Sample {
              private static final String TASK_ID = "taskId";
              protected ProvidedProcessEngineRule engineRule = new ProvidedProcessEngineRule();
              protected ProcessEngineTestRule testRule = new ProcessEngineTestRule(engineRule);
              TaskService taskService;
          
              @Test
              public void testDeleteTaskCommentWithoutAuthorization() {
                  // given
                  createTask(TASK_ID);
                  Comment createdComment = createComment(TASK_ID, null, "aComment");
                  var createdCommentId = createdComment.getId();
          
                  // when
                  try {
                      taskService.deleteTaskComment(TASK_ID, createdCommentId);
                      fail("Exception expected: It should not be possible to delete a comment.");
                  } catch (AuthorizationException e) {
                      // then
                      testRule.assertTextPresent(
                        "The user with id 'test' does not have one of the following permissions: 'TASK_WORK' permission on resource 'myTask' of type 'Task' or 'UPDATE' permission on resource 'myTask' of type 'Task'",
                        e.getMessage());
                  }
          
                  // triggers a db clean up
                  deleteTask(TASK_ID, true);
              }
          
              private void deleteTask(String taskId, boolean b) {
          
              }
          
              private Comment createComment(String taskId, Object o, String aComment) {
                  return null;
              }
          
              private void createTask(String taskId) {
          
              }
          
          }
          """));
    }

    @Test
    void rewriteTryCatchToAssertThatThrownBy() {
        rewriteRun(spec -> spec.cycles(4).expectedCyclesThatMakeChanges(1), java("""
          package org.operaton.rewrite.tests;
          
          import org.operaton.bpm.engine.AuthorizationException;
          import org.operaton.bpm.engine.query.PeriodUnit;
          import org.operaton.bpm.engine.runtime.ProcessInstance;
          import org.operaton.bpm.engine.test.api.authorization.AuthorizationTest;
          import static org.operaton.bpm.engine.authorization.Permissions.READ_HISTORY;
          import static org.operaton.bpm.engine.authorization.Resources.PROCESS_DEFINITION;
          
          import org.junit.Test;
          
          import static org.junit.Assert.fail;
          
          public class Sample extends AuthorizationTest {
              protected static final String PROCESS_KEY = "oneTaskProcess";
              protected static final String MESSAGE_START_PROCESS_KEY = "messageStartProcess";
          
              @Test
              public void testReportWithQueryCriterionProcessDefinitionIdInAndMissingReadHistoryPermission() {
                  // given
                  ProcessInstance processInstance1 = startProcessInstanceByKey(PROCESS_KEY);
                  ProcessInstance processInstance2 = startProcessInstanceByKey(MESSAGE_START_PROCESS_KEY);
                  disableAuthorization();
                  runtimeService.deleteProcessInstance(processInstance1.getProcessInstanceId(), "");
                  runtimeService.deleteProcessInstance(processInstance2.getProcessInstanceId(), "");
                  enableAuthorization();
          
                  createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, READ_HISTORY);
          
                  // when
                  try {
                      historyService
                        .createHistoricProcessInstanceReport()
                        .processDefinitionIdIn(processInstance1.getProcessDefinitionId(), processInstance2.getProcessDefinitionId())
                        .duration(PeriodUnit.MONTH);
          
                      // then
                      fail("Exception expected: It should not be possible to create a historic process instance report");
                  } catch (AuthorizationException e) {
          
                  }
              }
          }
          """, """
          package org.operaton.rewrite.tests;
          
          import org.operaton.bpm.engine.AuthorizationException;
          import org.operaton.bpm.engine.query.PeriodUnit;
          import org.operaton.bpm.engine.runtime.ProcessInstance;
          import org.operaton.bpm.engine.test.api.authorization.AuthorizationTest;
          import static org.operaton.bpm.engine.authorization.Permissions.READ_HISTORY;
          import static org.operaton.bpm.engine.authorization.Resources.PROCESS_DEFINITION;
          
          import org.junit.Test;
          
          import static org.junit.Assert.fail;
          
          public class Sample extends AuthorizationTest {
              protected static final String PROCESS_KEY = "oneTaskProcess";
              protected static final String MESSAGE_START_PROCESS_KEY = "messageStartProcess";
          
              @Test
              public void testReportWithQueryCriterionProcessDefinitionIdInAndMissingReadHistoryPermission() {
                  // given
                  ProcessInstance processInstance1 = startProcessInstanceByKey(PROCESS_KEY);
                  ProcessInstance processInstance2 = startProcessInstanceByKey(MESSAGE_START_PROCESS_KEY);
                  disableAuthorization();
                  runtimeService.deleteProcessInstance(processInstance1.getProcessInstanceId(), "");
                  runtimeService.deleteProcessInstance(processInstance2.getProcessInstanceId(), "");
                  enableAuthorization();
          
                  createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, READ_HISTORY);
                  var historicProcessInstanceReport = historyService
                    .createHistoricProcessInstanceReport()
                    .processDefinitionIdIn(processInstance1.getProcessDefinitionId(), processInstance2.getProcessDefinitionId());
          
                  // when
                  try {
                      historicProcessInstanceReport
                        .duration(PeriodUnit.MONTH);
          
                      // then
                      fail("Exception expected: It should not be possible to create a historic process instance report");
                  } catch (AuthorizationException e) {
          
                  }
              }
          }
          """));
    }

}
