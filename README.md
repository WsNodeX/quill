<div align="center">

# Quill

### 面向大模型的提示词工程工具包 · 零依赖 · 纯 Java 21

> 把散落在代码里的提示词，变成可版本化、可测试、可复用的一等公民。

[![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Dependencies](https://img.shields.io/badge/Dependencies-None-007ec6)](src/main/java)
[![Tests](https://img.shields.io/badge/Tests-62%20passing-brightgreen)](src/test/java)
[![Platform](https://img.shields.io/badge/Platform-JVM-blue)](src/main/java)

</div>

---

<p align="center">
  <a href="#-为什么需要-quill">✨ 为什么</a> ·
  <a href="#-核心特性">🚀 特性</a> ·
  <a href="#-安装">📦 安装</a> ·
  <a href="#-快速开始">⚡ 快速开始</a> ·
  <a href="#️-架构">🏗️ 架构</a> ·
  <a href="#-api-速查">🔌 API</a> ·
  <a href="#-路线图">🧭 路线图</a> ·
  <a href="#-中文技术科普文底稿">📝 中文长文</a> ·
  <a href="#-许可证">📄 许可证</a>
</p>

---

## ✨ 为什么需要 Quill

大模型应用里，**提示词（prompt）才是真正的核心逻辑**。但它在绝大多数项目里被写得极不讲究：

```java
String prompt = "你是一个助手。\n用户问：" + question + "\n请回答。";
```

这种"字符串拼接"有三个致命问题：

1. **不可维护**：变量一多，拼接符号和换行就糊成一团，改一处崩一片。
2. **不可测试**：提示词没有类型、没有校验，少传一个字段只能在线上才暴露。
3. **不可复用**：同一个"翻译""总结""问答"提示词在十个地方各抄一份，改文案要改十处。

Quill 的立场是：**提示词应该像代码一样被设计** —— 有模板、有变量约束、有单元测试、有版本号。它提供一套轻量但完整的工具：一个支持变量 / 过滤器 / 条件 / 循环的模板引擎，一套角色化的聊天消息模型，以及把提示词汇总成"可注册、可加载"的仓库（registry）。

最关键的一点：**零依赖**。整个库只用 JDK 标准库，自带一个极简 JSON 解析器，不引入任何第三方包。你把它当成一个 `quill/` 源码目录拖进项目即可，不会因为传递依赖和你的 Spring / Jakarta 版本打架。

---

## 🚀 核心特性

- **迷你模板引擎**：`{{ var }}`、点路径 `user.name`、下标 `items[0]`、过滤器 `{{ v | upper }}`、条件 `{% if %}`、循环 `{% for %}`、注释 `{# # }`。编译一次、多次渲染，确定且高效。
- **过滤器链**：内置 `upper / lower / trim / title / capitalize / default / maxlen / length / prefix / suffix`，参数写法 `{{ x | default: "n/a" }}`。
- **角色化聊天消息**：`system / user / assistant` 三种角色，直接映射 OpenAI、Anthropic、Mistral 等所有主流聊天接口的 `messages` 结构。
- **Few-shot 注入**：在 system 之后、真实用户消息之前自动插入示例，让模型"照着学"。
- **变量契约**：声明 `required` 变量与默认值，渲染前一次性校验，缺失即 fail-fast。
- **输出钩子（Hook）**：`trim / collapseWhitespace / maxLength / ensurePrefix` 等后置处理，统一收敛生成文本的形态。
- **提示词注册表**：用一份 JSON 把多个具名、带版本的提示词管理起来，像管理配置一样管理提示词。
- **离线可测**：不依赖任何网络调用，全部逻辑都能在单元测试里跑通。

---

## 📦 安装

### 方式一：直接拷贝源码（推荐，零依赖）

```bash
git clone https://github.com/WsNodeX/quill.git
# 把 src/main/java/quill 整个目录复制进你的项目即可，无需任何构建配置
```

### 方式二：作为模块编译

```bash
# 需要 JDK 21
javac -d out $(find src/main/java -name '*.java')

# 运行示例
java -cp out quill.examples.Demo

# 运行测试（需 junit-platform-console-standalone-1.10.2.jar）
javac -cp "out:junit.jar" -d out-test $(find src/test/java -name '*.java')
java -cp "out:out-test:junit.jar" \
  org.junit.platform.console.ConsoleLauncher --scan-class-path out-test
```

> Quill 没有任何运行时第三方依赖，也不强制使用 Maven / Gradle —— 一个 `javac` 就能编译运行。

---

## ⚡ 快速开始

### 1. 文本提示词

```java
import quill.core.*;
import java.util.*;

TemplateEngine engine = TemplateEngine.DEFAULT;

String template = """
        Dear {{ name | upper }},
        {% if vip %}
        Thanks for being a VIP member!
        {% else %}
        Consider upgrading to VIP.
        {% endif %}
        """;

Map<String, Object> vars = new LinkedHashMap<>();
vars.put("name", "ada lovelace");
vars.put("vip", true);

System.out.println(engine.render(template, vars));
```

### 2. 用 Prompt 管理带约束的提示词

```java
Prompt summarize = Prompt.builder("summarize")
        .version("1.0.0")
        .description("把文本总结成目标语言")
        .template("Summarize the following in {{ lang | default: \"English\" }}:\n\n{{ text }}")
        .variable(VariableSpec.required("text", "待总结文本"))
        .variable(VariableSpec.optional("lang", "English"))
        .hook(Hooks.trim())
        .build();

System.out.println(summarize.render(Map.of("text", "Quill is a small Java library.")));
```

### 3. 聊天提示词 + Few-shot

```java
Prompt qa = Prompt.builder("qa")
        .system("You are a concise maths tutor.")
        .user("Question: {{ q }}")
        .fewShot("What is 2+2?", "4")
        .fewShot("What is 5*3?", "15")
        .build();

for (ChatMessage m : qa.renderChat(Map.of("q", "What is 12/4?"))) {
    System.out.println("[" + m.role().wire() + "] " + m.content());
}
```

### 4. 运行内置 Demo（预期输出）

```
=== 1. Variables, filters and control flow ===
Dear ADA LOVELACE,

Thanks for being a VIP member!

Your orders:
1. Calculus book
2. Abacus
3. Notebook

=== 2. Text prompt with required variables + a trim hook ===
Summarize the following in English:

Quill is a small Java library for composing LLM prompts.

=== 3. Chat prompt with few-shot examples ===
[system] You are a concise maths tutor.
[user] What is 2+2?
[assistant] 4
[user] What is 5*3?
[assistant] 15
[user] Question: What is 12/4?

=== 4. Prompt registry loaded from JSON ===
Hello World!
Registered prompts: [greet]
```

---

## 🏗️ 架构

Quill 采用分层设计，每一层职责单一、可独立使用：

```
┌─────────────────────────────────────────────────────────┐
│                      quill.cli.Main                      │  命令行入口
├─────────────────────────────────────────────────────────┤
│                   quill.registry                          │  提示词注册表（JSON 加载）
│   PromptRegistry.loadFromJson / loadFromFile / register   │
├─────────────────────────────────────────────────────────┤
│                     quill.core.Prompt                     │  提示词抽象（文本 / 聊天）
│   变量契约 · 默认值 · Hook 后置 · Few-shot 编排            │
├─────────────────────────────────────────────────────────┤
│                  quill.core.TemplateEngine                 │  模板引擎
│   Lexer ──▶ Parser ──▶ AST(Node) ──▶ Renderer            │
│   {{ }}  {% if %}  {% for %}  | filter                  │
├─────────────────────────────────────────────────────────┤
│   quill.core.RenderContext  ·  Filters  ·  Hooks  ·  Role  │  渲染上下文 / 过滤器 / 钩子 / 角色
├─────────────────────────────────────────────────────────┤
│                   quill.json.Json                         │  零依赖 JSON 读写（供 registry 使用）
└─────────────────────────────────────────────────────────┘
```

渲染一次模板的执行路径：词法分析切出 `TEXT / VAR / TAG / COMMENT` 四类 token → 语法分析递归构造 AST（文本节点、变量节点、条件节点、循环节点）→ 渲染时由 `RenderContext` 解析变量（支持点路径与下标、循环帧作用域），最后逐个节点拼装字符串并应用过滤器与 Hook。

---

## 🔌 API 速查

| 入口 | 方法 | 说明 |
| --- | --- | --- |
| `TemplateEngine` | `compile(String)` → `Template` | 把模板源码编译成可复用的 AST |
| `TemplateEngine` | `render(String, Map)` | 编译并立即渲染 |
| `Template` | `render(Map<String,Object>)` | 用变量表渲染已编译模板 |
| `Prompt.Builder` | `template / system / user / variable / hook / fewShot` | 流式构建提示词 |
| `Prompt` | `render(Map)` / `renderChat(Map)` | 渲染为文本 / 聊天消息列表 |
| `PromptRegistry` | `loadFromJson / loadFromFile / register` | 批量加载或注册提示词 |
| `PromptRegistry` | `render(name, vars)` / `renderChat(name, vars)` | 按名字渲染 |
| `Filters` | `upper / lower / trim / title / capitalize / default / maxlen / length` | 内置过滤器 |
| `Hooks` | `trim / collapseWhitespace / maxLength / ensurePrefix / ensureSuffix` | 内置后置钩子 |
| `Role` | `SYSTEM / USER / ASSISTANT` | 聊天角色，含 `wire()` 接口串 |

**模板语法一览**

| 语法 | 含义 |
| --- | --- |
| `{{ name }}` | 变量替换 |
| `{{ user.name }}` / `{{ items[0] }}` | 点路径 / 下标访问 |
| `{{ v \| upper }}` | 过滤器；可链式 `{{ v \| trim \| maxlen: 10 }}` |
| `{{ v \| default: "x" }}` | 变量为空时回退 |
| `{% if cond %}…{% else %}…{% endif %}` | 条件；支持 `== != > >= < <=` 比较 |
| `{% for item in list %}…{% endfor %}` | 循环；体内可用 `loop.index / loop.first / loop.last` |
| `{# 注释 #}` | 渲染时剔除 |

---

## 🧭 路线图

- [ ] 多语言模板资源（按 locale 选择提示词）
- [ ] 变量类型校验（数字范围、枚举白名单、正则约束）
- [ ] 提示词评测钩子（自动跑回归集，对比输出漂移）
- [ ] 与 OpenAI / Anthropic SDK 的薄适配层示例
- [ ] Maven Central 发布（带零依赖 pom）
- [ ] 模板缓存与预热（高频渲染场景）
- [ ] 交互式提示词调试器（CLI 预览 + 差异对比）

---

## 🤝 贡献

欢迎提 Issue 与 PR。本地开发只需 JDK 21：

```bash
javac -d out $(find src/main/java -name '*.java')
javac -cp "out:junit.jar" -d out-test $(find src/test/java -name '*.java')
java -cp "out:out-test:junit.jar" org.junit.platform.console.ConsoleLauncher --scan-class-path out-test
```

提交的测试应当全绿；新增能力请配套单元测试。

---

## 📄 许可证

[MIT](LICENSE) © WsNodeX

---

# 📝 中文技术科普文底稿（可直接发掘金 / 知乎 / 小红书 / B 站）

> 下面是一篇可直接发布的科普文。发布时把代码块换成你自己的截图或运行录像即可。

## 标题建议
**《我把散落在项目各处的 AI 提示词，重写成了"可测试的代码"》**

## 一、痛点：提示词正在悄悄变成技术债

做过大模型应用的同学一定有体会：一开始提示词就一行 `String prompt = "你是一个助手：" + input`，写着写着，变量越来越多，换行和加号糊成一团，改一个字段要全项目搜。更糟的是，**提示词没有类型、没有校验**——少传一个参数，程序不会在编译期报错，只会在用户线上提问时才崩。

这本质上是把"产品逻辑"藏进了字符串里。本文介绍一个思路：用一套轻量模板引擎，把提示词工程化。

## 二、核心原理

提示词渲染，可以分解为四步：

1. **词法切分**：把模板拆成"纯文本 / 变量 / 控制标签 / 注释"四类片段。例如 `{{ name }}` 是变量，`{% if vip %}` 是控制标签。
2. **语法分析**：把片段递归组装成抽象语法树（AST）。遇到 `if/for` 就生成条件节点或循环节点，遇到 `endif/endfor` 就收口。
3. **变量解析**：渲染时按名字查值，支持 `user.name` 这种点路径、`items[0]` 这种下标，以及 `for` 循环里的局部作用域（`item`、`loop.index`）。
4. **后处理**：渲染完的字符串再过一遍"过滤器"和"钩子"，比如统一 `trim`、限制最大长度、转大写。

关键在于：**编译一次、多次渲染**，渲染过程不依赖任何网络，因此每一段提示词逻辑都能写单元测试。

## 三、几个面试官爱问的实现点

- **循环为什么会栈溢出？** 解析 `if/for` 时，必须先"消费"掉开头的标签再递归解析子块，否则解析器会反复在同一个标签上无限递归。
- **`default` 过滤器如何兼容"变量不存在"？** 严格模式下未定义变量应当报错（防拼写错误），但 `default` 又需要为缺失变量兜底——做法是：当表达式带 `default` 过滤器时才允许缺失，其余情况仍 fail-fast。
- **点路径访问怎么支持 Java Bean？** 先查 Map，再反射找 `getXxx()` / `isXxx()`，最后退而求其次读 public 字段。
- **零依赖 JSON 解析器值得写吗？** 为了不引入第三方库，可以手写一个只覆盖对象 / 数组 / 字符串 / 数字 / 布尔 / null 的最小递归下降解析器，足以描述提示词注册表。

## 四、与同类方案对比

| 维度 | 手写字符串拼接 | 通用模板引擎（如 FreeMarker） | Quill |
| --- | --- | --- | --- |
| 依赖体积 | 0 | 重（还需适配） | 0（仅 JDK） |
| 聊天消息模型 | 自己拼 | 没有 | 内置 system/user/assistant |
| Few-shot 编排 | 自己拼 | 没有 | 内置 |
| 变量约束 / 测试 | 无 | 弱 | 声明式 + 单元测试 |
| 上手成本 | 低但难维护 | 高 | 低 |

结论：如果你的提示词只是偶尔拼一下，字符串拼接够了；但如果提示词是产品核心逻辑，值得用一套"轻量 + 零依赖 + 可测"的方案把它管起来。

## 五、如何扩展成一篇发布稿

- **引流钩子**：开头抛一个真实踩坑故事（线上因少传字段崩了）。
- **配图**：把"拼接 vs 模板"两段代码并排截图，直观对比。
- **互动**：结尾留一个投票"你项目里的提示词是怎么管的？A 拼接 B 配置 C 框架"。
- **代码跑通**：放一段 `java quill.examples.Demo` 的运行录屏，增强可信度。
- **下集预告**：下一期讲"如何给提示词写回归测试，防止模型输出悄悄变差"。
