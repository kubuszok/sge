package sge
package ai
package btree
package utils

import sge.ai.btree.branch.{ Selector, Sequence }
import sge.ai.btree.decorator.Invert
import sge.ai.btree.leaf.{ Failure => FailureLeaf, Success => SuccessLeaf }
import sge.utils.Nullable

class BehaviorTreeParserSuite extends munit.FunSuite {

  /** Create a parser with a task registry pre-loaded with built-in and custom task types. */
  private def makeParser(): BehaviorTreeParser[String] = {
    val registry = new BehaviorTreeParser.TaskRegistry()

    // Register built-in tasks using the fully qualified names that defaultImports resolves to
    registry.registerTask("sge.ai.btree.branch.Selector", () => new Selector[String]())
    registry.registerTask("sge.ai.btree.branch.Sequence", () => new Sequence[String]())
    registry.registerTask("sge.ai.btree.leaf.Success", () => new SuccessLeaf[String]())
    registry.registerTask("sge.ai.btree.leaf.Failure", () => new FailureLeaf[String]())
    registry.registerTask(
      "sge.ai.btree.decorator.Invert",
      () => new Invert[String](Nullable.empty[Task[String]])
    )

    // Register custom test tasks
    registry.registerTask(
      "mySuccess",
      () => new LeafTask[String] {
        override def execute(): Task.Status = Task.Status.SUCCEEDED
        override def newInstance(): Task[String] = throw new UnsupportedOperationException
        override protected def copyTo(task: Task[String]): Task[String] = task
      }
    )
    registry.registerTask(
      "myFail",
      () => new LeafTask[String] {
        override def execute(): Task.Status = Task.Status.FAILED
        override def newInstance(): Task[String] = throw new UnsupportedOperationException
        override protected def copyTo(task: Task[String]): Task[String] = task
      }
    )
    registry.registerTask(
      "myTask",
      () => new LeafTask[String] {
        override def execute(): Task.Status = Task.Status.SUCCEEDED
        override def newInstance(): Task[String] = throw new UnsupportedOperationException
        override protected def copyTo(task: Task[String]): Task[String] = task
      },
      BehaviorTreeParser.TaskMeta(
        attributes = Map(
          "value" -> BehaviorTreeParser.AttrInfo("value", (_, _) => {
            // Accept the value (we just verify parsing works)
          })
        )
      )
    )

    val reader = new BehaviorTreeParser.DefaultBehaviorTreeReader[String](taskRegistry = registry)
    new BehaviorTreeParser[String](btReaderOpt = Nullable(reader))
  }

  // ── Simple tree parsing ──────────────────────────────────────────────

  test("parse single leaf task") {
    val parser = makeParser()
    val tree = parser.parse("mySuccess", Nullable("bb"))
    tree.step()
    assertEquals(tree.getStatus, Task.Status.SUCCEEDED)
  }

  test("parse selector with children") {
    val parser = makeParser()
    val input =
      """selector
        |  myFail
        |  mySuccess""".stripMargin
    val tree = parser.parse(input, Nullable("bb"))
    tree.step()
    assertEquals(tree.getStatus, Task.Status.SUCCEEDED)
  }

  test("parse sequence with children") {
    val parser = makeParser()
    val input =
      """sequence
        |  mySuccess
        |  mySuccess""".stripMargin
    val tree = parser.parse(input, Nullable("bb"))
    tree.step()
    assertEquals(tree.getStatus, Task.Status.SUCCEEDED)
  }

  test("parse sequence that fails") {
    val parser = makeParser()
    val input =
      """sequence
        |  mySuccess
        |  myFail""".stripMargin
    val tree = parser.parse(input, Nullable("bb"))
    tree.step()
    assertEquals(tree.getStatus, Task.Status.FAILED)
  }

  // ── Nested tree ──────────────────────────────────────────────────────

  test("parse nested selector inside sequence") {
    val parser = makeParser()
    val input =
      """sequence
        |  selector
        |    myFail
        |    mySuccess
        |  mySuccess""".stripMargin
    val tree = parser.parse(input, Nullable("bb"))
    tree.step()
    assertEquals(tree.getStatus, Task.Status.SUCCEEDED)
  }

  // ── Task with attribute ──────────────────────────────────────────────

  test("parse task with attribute") {
    val parser = makeParser()
    val input = """myTask value:42"""
    val tree = parser.parse(input, Nullable("bb"))
    tree.step()
    assertEquals(tree.getStatus, Task.Status.SUCCEEDED)
  }

  // ── Built-in tasks ───────────────────────────────────────────────────

  test("parse built-in success leaf") {
    val parser = makeParser()
    val input = "success"
    val tree = parser.parse(input, Nullable("bb"))
    tree.step()
    assertEquals(tree.getStatus, Task.Status.SUCCEEDED)
  }

  test("parse built-in failure leaf") {
    val parser = makeParser()
    val input = "failure"
    val tree = parser.parse(input, Nullable("bb"))
    tree.step()
    assertEquals(tree.getStatus, Task.Status.FAILED)
  }

  test("parse built-in invert decorator") {
    val parser = makeParser()
    val input =
      """invert
        |  mySuccess""".stripMargin
    val tree = parser.parse(input, Nullable("bb"))
    tree.step()
    assertEquals(tree.getStatus, Task.Status.FAILED)
  }

  // ── Blackboard ───────────────────────────────────────────────────────

  test("parsed tree has blackboard object") {
    val parser = makeParser()
    val tree = parser.parse("mySuccess", Nullable("hello world"))
    assertEquals(tree.getObject, "hello world")
  }

  test("parsed tree with null blackboard") {
    val parser = makeParser()
    val tree = parser.parse("mySuccess", Nullable.empty[String])
    tree.step()
    assertEquals(tree.getStatus, Task.Status.SUCCEEDED)
  }

  // ── Error handling ───────────────────────────────────────────────────

  test("parse unknown task throws") {
    val parser = makeParser()
    intercept[Exception] {
      parser.parse("unknownTask", Nullable("bb"))
    }
  }

  test("parse empty string throws") {
    val parser = makeParser()
    intercept[Exception] {
      parser.parse("", Nullable("bb"))
    }
  }
}
