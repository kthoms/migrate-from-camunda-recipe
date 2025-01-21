package org.operaton.rewrite.tests;

import java.util.*;
import java.util.stream.Collectors;

import lombok.NonNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import static java.util.Collections.emptyList;

public class TryCatchExtractCallArgsToVariable extends Recipe {
  @Override
  public String getDisplayName() {
    return "Extract call arguments to variable in try-catch blocks";
  }

  @Override
  public String getDescription() {
    return "Find methods annotated with @Test that contain a try-catch statement where the try-block contains a fail() call, and extract call arguments to variables.";
  }

  @Override
  public JavaIsoVisitor<ExecutionContext> getVisitor() {
    return new JavaIsoVisitor<ExecutionContext>() {
      @Override
      public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
        if (method.getLeadingAnnotations()
          .stream()
          .anyMatch(ann -> ann.getType().toString().equals("org.junit.Test") || ann.getType()
            .toString()
            .equals("org.junit.jupiter.api.Test"))) {
          return super.visitMethodDeclaration(method, ctx);
        }
        return method;
      }

      @Override
      public @NonNull J.Try visitTry(@NonNull J.Try tryable, @NonNull ExecutionContext ctx) {
        if (!getStatementBeforeFail(tryable).isPresent()) {
          return tryable;
        }
        J.MethodInvocation statementBeforeFail = getStatementBeforeFail(tryable).get();
        Map<String, J.MethodInvocation> newVariables = new HashMap<>();

        List<Expression> arguments = statementBeforeFail.getArguments();
        if (arguments.stream().noneMatch(arg -> arg instanceof J.MethodInvocation)) {
          return tryable;
        }

        List<Expression> newArguments = new ArrayList<>();
        for (int i = 0; i < arguments.size(); i++) {
          if (arguments.get(i) instanceof J.MethodInvocation) {
            J.MethodInvocation methodInvocation = (J.MethodInvocation) arguments.get(i);
            String variableName = computeVariableName(methodInvocation);
            newArguments.add(
              new J.Identifier(Tree.randomId(), i == 0 ? Space.EMPTY : Space.SINGLE_SPACE, Markers.EMPTY, emptyList(),
                variableName, null, null));

            newVariables.put(variableName, methodInvocation);
          } else {
            newArguments.add(arguments.get(i));
          }
        }

        statementBeforeFail = statementBeforeFail
          .withArguments(newArguments);

        Optional<TextComment> whenComment = statementBeforeFail
          .getPrefix()
          .getComments()
          .stream()
          .filter(TextComment.class::isInstance)
          .map(TextComment.class::cast)
          .filter(comment -> comment.getText().trim().equals("when"))
          .findFirst();
        if (whenComment.isPresent()) {
          List<Comment> comments = tryable.getComments();
          comments.add(new TextComment(false, " when", "\n" + tryable.getPrefix().getIndent(), Markers.EMPTY));
          tryable = tryable.withComments(comments);

          statementBeforeFail = statementBeforeFail.withComments(emptyList());
        }

        J.MethodInvocation finalStatementBeforeFail = statementBeforeFail;

        tryable = tryable.withBody(tryable.getBody()
          .withStatements(tryable.getBody()
            .getStatements()
            .stream()
            .map(stmt -> stmt == finalStatementBeforeFail ? finalStatementBeforeFail : stmt)
            .collect(Collectors.toList())));

        tryable = tryable.withBody(
          replaceStatement(tryable.getBody(), statementBeforeFail, statementBeforeFail));

        if (!newVariables.isEmpty()) {
          getCursor().dropParentUntil(J.Block.class::isInstance).putMessage("newVariable", newVariables);
        }

        return super.visitTry(tryable, ctx);
      }

      private Optional<J.MethodInvocation> getStatementBeforeFail(J.Try tryable) {
        Optional<J.MethodInvocation> failStatement = tryable.getBody()
          .getStatements()
          .stream()
          .filter(stmt -> stmt instanceof J.MethodInvocation)
          .map(stmt -> (J.MethodInvocation) stmt)
          .filter(mi -> mi.getSimpleName().equals("fail"))
          .findFirst();

        Optional<Statement> statementBeforeFail = failStatement.map(
          fail -> tryable.getBody().getStatements().get(tryable.getBody().getStatements().indexOf(fail) - 1));
        return statementBeforeFail.map(J.MethodInvocation.class::cast);
      }

      private J.Block replaceStatement(J.Block block, Statement statement, Statement newStatement) {
        return block.withStatements(block.getStatements()
          .stream()
          .map(stmt -> stmt == statement ? newStatement : stmt)
          .collect(Collectors.toList()));
      }

      private String computeVariableName(J.MethodInvocation methodInvocation) {
        if (methodInvocation.toString().matches("created(\\w+)\\.getId\\(\\)")) {
          return methodInvocation.toString().replaceFirst("created(\\w+)\\.getId\\(\\)", "created$1Id");
        }
        String methodName = methodInvocation.getSimpleName();
        return methodName + "Result";
      }

      private Statement moveWhenComment (Statement statementBeforeFail, J.Try tryable) {
        Optional<TextComment> whenComment = statementBeforeFail
          .getPrefix()
          .getComments()
          .stream()
          .filter(TextComment.class::isInstance)
          .map(TextComment.class::cast)
          .filter(comment -> comment.getText().trim().equals("when"))
          .findFirst();
        if (whenComment.isPresent()) {
          List<Comment> comments = tryable.getComments();
          comments.add(new TextComment(false, " when", "\n" + tryable.getPrefix().getIndent(), Markers.EMPTY));
          tryable = tryable.withComments(comments);

          statementBeforeFail = statementBeforeFail.withComments(emptyList());
        }
        return statementBeforeFail;
      }

      @Override
      public @NonNull J.Block visitBlock(@NonNull J.Block block, @NonNull ExecutionContext ctx) {
        block = super.visitBlock(block, ctx);
        Map<String, J.MethodInvocation> newVariables = getCursor().pollMessage("newVariable");
        if (newVariables != null) {
          List<Statement> statements = new ArrayList<>(block.getStatements());
          List<Statement> newVariableDeclarations = new ArrayList<>();
          // Find the index of the first try statement
          int tryIndex = -1;
          for (int i = 0; i < statements.size(); i++) {
            if (statements.get(i) instanceof J.Try) {
              tryIndex = i;
              break;
            }
          }

          for (Map.Entry<String, J.MethodInvocation> entry : newVariables.entrySet()) {
            String variableName = entry.getKey();
            J.MethodInvocation methodInvocation = entry.getValue();

            List<JRightPadded<J.VariableDeclarations.NamedVariable>> variables = new ArrayList<>();

            J.Identifier name = new J.Identifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY, null, variableName, null, null);
            J.VariableDeclarations.NamedVariable namedVariable = new J.VariableDeclarations.NamedVariable(
              Tree.randomId(),
              Space.SINGLE_SPACE,
              Markers.EMPTY,
              name,
              emptyList(), // Ensure dimensionsAfterName is an empty list
              JLeftPadded.build(methodInvocation),
              null
            );
            variables.add(JRightPadded.build(namedVariable));


            J.VariableDeclarations variableDeclarations = new J.VariableDeclarations(
              Tree.randomId(),
              Space.build("\n" + statements.get(tryIndex).getPrefix().getIndent(), emptyList()),
              Markers.EMPTY,
              emptyList(),
              emptyList(),
              TypeTree.build("var"),
              null,
              emptyList(),
              variables
            );

            newVariableDeclarations.add(variableDeclarations);
          }


          if (tryIndex != -1) {
            statements.addAll(tryIndex, newVariableDeclarations);
            block = block.withStatements(statements);
          }
        }
        return block;
      }
    };

  }

}