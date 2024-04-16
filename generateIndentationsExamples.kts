import java.io.File
import java.nio.file.Files

val file = File(System.getProperty("user.dir") + "/vault/IndentationsExamples.md")
if (file.exists()) file.delete()
file.createNewFile()

var content = "# Indentations Examples text+task\n"
for(i in 0..<9) {
  for (j in 0..<9) {
    content += "### $i-text + $j-task\n"
    content += "- [ ] 0-level task\n"
    content += " ".repeat(i) + "$i-level text\n"
    content += " ".repeat(j) + "- [ ] $j-level task\n"
  }
}

content += "# Indentations Examples task+task\n"
for(i in 0..<9) {
  for (j in 0..<9) {
    content += "### $i-text + $j-task\n"
    content += " ".repeat(i) + "- [ ] $i-level task\n"
    content += " ".repeat(j) + "- [ ] $j-level task\n"
  }
}

file.writeText(content)