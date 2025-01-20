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
    void rewriteTryCatchToAssertThatThrownBy() {
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
          
                  try {
                      // when
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

}
