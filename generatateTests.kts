import java.io.File
import java.nio.file.Files


val testDir = File(System.getProperty("user.dir") + "/vault/generatedTests")
if (testDir.exists()) {
  Files.walk(testDir.toPath())
    .sorted(Comparator.reverseOrder())
    .map { it.toFile() }
    .forEach { it -> it.delete() };
}
testDir.mkdir()

val grouping = listOf(
//  listOf(1, 1, 1),
//  listOf(2, 1),
//  listOf(3),

//  listOf(1, 1, 1, 1),
//  listOf(2, 1, 1),
//  listOf(2, 2),
//  listOf(3, 1),
//  listOf(4),

  listOf(1, 1, 1, 1, 1),
  listOf(2, 1, 1, 1),
  listOf(2, 2, 1),
  listOf(3, 1, 1),
  listOf(3, 2),
  listOf(4, 1),
  listOf(5),
)

val allAllocations = grouping
  .flatMap { arr ->
    val indents = List(arr.size) { it + 1 }
    arr.combinations()
      .map { it.zip(indents).flatMap { (a, b) -> List(a) { b } } }
      .flatMap { it.combinations() }
  }

for (allocation in allAllocations) {
  if (!isValidAllocation(allocation)) {
    continue;
  }

  val tasks = allocation.mapIndexed { index, indent ->
    Task("task" + (index + 1), index + 1, indent - 1)
  }

  val content = StringBuffer()
//  content.nl("# Tasks with ${allocation.joinToString(", ")} allocation")
//  content.nl()

  for (task in tasks) {
    content.nl("    ".repeat(task.indent) + "- [ ] " + task.name + " #indent_${task.indent}")
  }


  content.nl("# Search tests")
  for (task in tasks) {
//    content.nl("### ${task.name}")
    val relation = defineRelation(task, tasks)

    var filter: String = ""
    filter += " && " + if (relation.next != null) {
      "task.nextSibling != null && task.nextSibling.descriptionWithoutTags == \"${relation.next}\""
    } else {
      "task.nextSibling == null"
    }
    filter += " && " + if (relation.prev != null) {
      "task.previousSibling != null && task.previousSibling.descriptionWithoutTags == \"${relation.prev}\""
    } else {
      "task.previousSibling == null"
    }
    filter += " && " + if (relation.parent != null) {
      "task.parent != null && task.parent.descriptionWithoutTags == \"${relation.parent}\""
    } else {
      "task.parent == null"
    }
    filter += " && task.children.length == ${relation.children.size}"
    for ((idx, child) in relation.children.withIndex()) {
      filter += " && task.children[$idx].descriptionWithoutTags == \"$child\""
    }
    appendSearch(task, content) { filter }


    content.nl("> [!INFO]- Details")
    content.nl("> ###    ${task.name}.nextSibling, expected ${relation.next}")
    appendSearch(task, content, "> ") {
      " && " + if (relation.next != null) {
        "task.nextSibling != null && task.nextSibling.descriptionWithoutTags == \"${relation.next}\""
      } else {
        "task.nextSibling == null"
      }
    }
    content.nl("> ###    ${task.name}.previousSibling, expected ${relation.prev}")
    appendSearch(task, content, "> ") {
      " && " + if (relation.prev != null) {
        "task.previousSibling != null && task.previousSibling.descriptionWithoutTags == \"${relation.prev}\""
      } else {
        "task.previousSibling == null"
      }
    }
    content.nl("> ###    ${task.name}.parent, expected ${relation.parent}")
    appendSearch(task, content, "> ") {
      " && " + if (relation.parent != null) {
        "task.parent != null && task.parent.descriptionWithoutTags == \"${relation.parent}\""
      } else {
        "task.parent == null"
      }
    }
    content.nl("> ###    ${task.name}.children.length, expected ${relation.children.size}")
    appendSearch(task, content, "> ") { " && task.children.length == ${relation.children.size}" }
    for ((idx, child) in relation.children.withIndex()) {
      content.nl("> ###    ${task.name}.children[$idx], expected ${child}")
      appendSearch(task, content, "> ") { " && task.children[$idx].descriptionWithoutTags == \"$child\"" }
    }


  }

  val testFile = File(testDir, "Test_${allocation.joinToString("")}.md")
  testFile.createNewFile()
  testFile.writeText(content.toString())
}



fun isValidAllocation(alloc: List<Int>): Boolean {
  var prev = 0
  for (a in alloc) {
    if (a - prev > 1) {
      return false
    }
    prev = a
  }
  return true
}

fun appendSearch(task: Task, content: StringBuffer, prefix: String = "", condition: () -> String) {
  var filter: String = "query.file.path == task.taskLocation.path"
  filter += " && task.descriptionWithoutTags == \"${task.name}\""
  filter += condition()

  content.nl(
    """
       |$prefix```tasks
       |filter by function $filter
       |$prefix```
     """.trimMargin()
  )
}


data class Task(
  val name: String,
  val index: Int,
  val indent: Int,
)

data class Relation(
  val task: String,
  val parent: String? = null,
  val children: List<String> = emptyList(),
  val next: String? = null,
  val prev: String? = null,
)


fun defineRelation(task: Task, tasks: List<Task>): Relation {
  val id = tasks.map { it.indent + 1 }.joinToString("") + "/" + task.indent
  val before = tasks.filter { it.index < task.index }
  val after = tasks.filter { it.index > task.index }

  val parent = before.reversed().filter { it.indent < task.indent }.firstOrNull()

  val children: MutableList<Task> = mutableListOf()
  var childrenMinIndent = Int.MAX_VALUE
  for (afterTask in after) {
    childrenMinIndent = Math.min(childrenMinIndent, afterTask.indent)
    if (childrenMinIndent <= task.indent) break
    if (afterTask.indent == childrenMinIndent) {
      children.add(afterTask)
    }
  }

  var prevSibling: Task? = null;
  for (beforeTask in before.reversed()) {
    if (beforeTask.indent == task.indent) {
      prevSibling = beforeTask
      break
    }
    if (beforeTask.indent < task.indent) {
      break
    }
  }

  var nextSibling: Task? = null;
  for (afterTask in after) {
    if (afterTask.indent == task.indent) {
      nextSibling = afterTask
      break
    }
    if (afterTask.indent < task.indent) {
      break
    }
  }

  return Relation(
    task = task.name,
    parent = parent?.name,
    children = children.map { it.name },
    next = nextSibling?.name,
    prev = prevSibling?.name,
  )
}

fun StringBuffer.nl(text: String = "") {
  append(text)
  append("\n")
}

fun <T> combinationsFn(list: List<T>): List<List<T>> {
  fun combinationsFn(tail: List<T>, head: List<T>): List<List<T>> {
    if (tail.isEmpty()) return listOf(head)
    return tail.flatMapIndexed { i, t ->
      combinationsFn(tail.take(i) + tail.drop(i + 1), head + t)
    }
  }

  return combinationsFn(list, emptyList()).distinct()
}

fun <T> List<T>.combinations(): List<List<T>> = combinationsFn(this)