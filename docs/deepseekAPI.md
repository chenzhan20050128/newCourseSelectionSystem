首次调用 API
DeepSeek API 使用与 OpenAI 兼容的 API 格式，通过修改配置，您可以使用 OpenAI SDK 来访问 DeepSeek API，或使用与 OpenAI API 兼容的软件。

PARAM	VALUE
base_url *       	https://api.deepseek.com
api_key	apply for an API key
* 出于与 OpenAI 兼容考虑，您也可以将 base_url 设置为 https://api.deepseek.com/v1 来使用，但注意，此处 v1 与模型版本无关。

* deepseek-chat 和 deepseek-reasoner 都已经升级为 DeepSeek-V3.2-Exp。deepseek-chat 对应 DeepSeek-V3.2-Exp 的非思考模式，deepseek-reasoner 对应 DeepSeek-V3.2-Exp 的思考模式。

调用对话 API
在创建 API key 之后，你可以使用以下样例脚本的来访问 DeepSeek API。样例为非流式输出，您可以将 stream 设置为 true 来使用流式输出。

curl
python
nodejs
curl https://api.deepseek.com/chat/completions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${DEEPSEEK_API_KEY}" \
  -d '{
        "model": "deepseek-chat",
        "messages": [
          {"role": "system", "content": "You are a helpful assistant."},
          {"role": "user", "content": "Hello!"}
        ],
        "stream": false
      }'

      在创建 API key 之后，你可以使用以下样例脚本的来访问 DeepSeek API。样例为非流式输出，您可以将 stream 设置为 true 来使用流式输出。

curl
python
nodejs
# Please install OpenAI SDK first: `pip3 install openai`
import os
from openai import OpenAI

client = OpenAI(
    api_key=os.environ.get('DEEPSEEK_API_KEY'),
    base_url="https://api.deepseek.com")

response = client.chat.completions.create(
    model="deepseek-chat",
    messages=[
        {"role": "system", "content": "You are a helpful assistant"},
        {"role": "user", "content": "Hello"},
    ],
    stream=False
)

print(response.choices[0].message.content)


根据输入的上下文，来让模型补全对话内容。

Request
application/json
Body

required

messages

object[]

required

Possible values: >= 1

对话的消息列表。

Array [

oneOf

System message
User message
Assistant message
Tool message
content
string
required
system 消息的内容。

role
string
required
Possible values: [system]

该消息的发起角色，其值为 system。

name
string
可以选填的参与者的名称，为模型提供信息以区分相同角色的参与者。

]

model
string
required
Possible values: [deepseek-chat, deepseek-reasoner]

使用的模型的 ID。您可以使用 deepseek-chat。

frequency_penalty
number
nullable
Possible values: >= -2 and <= 2

Default value: 0

介于 -2.0 和 2.0 之间的数字。如果该值为正，那么新 token 会根据其在已有文本中的出现频率受到相应的惩罚，降低模型重复相同内容的可能性。

max_tokens
integer
nullable
限制一次请求中模型生成 completion 的最大 token 数。输入 token 和输出 token 的总长度受模型的上下文长度的限制。取值范围与默认值详见文档。

presence_penalty
number
nullable
Possible values: >= -2 and <= 2

Default value: 0

介于 -2.0 和 2.0 之间的数字。如果该值为正，那么新 token 会根据其是否已在已有文本中出现受到相应的惩罚，从而增加模型谈论新主题的可能性。

response_format

object

nullable

stop

object

nullable

stream
boolean
nullable
如果设置为 True，将会以 SSE（server-sent events）的形式以流式发送消息增量。消息流以 data: [DONE] 结尾。

stream_options

object

nullable

流式输出相关选项。只有在 stream 参数为 true 时，才可设置此参数。

include_usage
boolean
如果设置为 true，在流式消息最后的 data: [DONE] 之前将会传输一个额外的块。此块上的 usage 字段显示整个请求的 token 使用统计信息，而 choices 字段将始终是一个空数组。所有其他块也将包含一个 usage 字段，但其值为 null。

temperature
number
nullable
Possible values: <= 2

Default value: 1

采样温度，介于 0 和 2 之间。更高的值，如 0.8，会使输出更随机，而更低的值，如 0.2，会使其更加集中和确定。 我们通常建议可以更改这个值或者更改 top_p，但不建议同时对两者进行修改。

top_p
number
nullable
Possible values: <= 1

Default value: 1

作为调节采样温度的替代方案，模型会考虑前 top_p 概率的 token 的结果。所以 0.1 就意味着只有包括在最高 10% 概率中的 token 会被考虑。 我们通常建议修改这个值或者更改 temperature，但不建议同时对两者进行修改。

tools

object[]

nullable

tool_choice

object

nullable

logprobs
boolean
nullable
是否返回所输出 token 的对数概率。如果为 true，则在 message 的 content 中返回每个输出 token 的对数概率。

top_logprobs
integer
nullable
Possible values: <= 20

一个介于 0 到 20 之间的整数 N，指定每个输出位置返回输出概率 top N 的 token，且返回这些 token 的对数概率。指定此参数时，logprobs 必须为 true。

Responses
200 (No streaming)
200 (Streaming)
OK, 返回一个 chat completion 对象。

application/json
Schema
Example (from schema)
Example
Schema

id
string
required
该对话的唯一标识符。

choices

object[]

required

模型生成的 completion 的选择列表。

Array [

finish_reason
string
required
Possible values: [stop, length, content_filter, tool_calls, insufficient_system_resource]

模型停止生成 token 的原因。

stop：模型自然停止生成，或遇到 stop 序列中列出的字符串。

length ：输出长度达到了模型上下文长度限制，或达到了 max_tokens 的限制。

content_filter：输出内容因触发过滤策略而被过滤。

insufficient_system_resource：系统推理资源不足，生成被打断。

index
integer
required
该 completion 在模型生成的 completion 的选择列表中的索引。

message

object

required

logprobs

object

nullable

required

]

created
integer
required
创建聊天完成时的 Unix 时间戳（以秒为单位）。

model
string
required
生成该 completion 的模型名。

system_fingerprint
string
required
This fingerprint represents the backend configuration that the model runs with.

object
string
required
Possible values: [chat.completion]

对象的类型, 其值为 chat.completion。

usage

object

该对话补全请求的用量信息。

completion_tokens
integer
required
模型 completion 产生的 token 数。

prompt_tokens
integer
required
用户 prompt 所包含的 token 数。该值等于 prompt_cache_hit_tokens + prompt_cache_miss_tokens

prompt_cache_hit_tokens
integer
required
用户 prompt 中，命中上下文缓存的 token 数。

prompt_cache_miss_tokens
integer
required
用户 prompt 中，未命中上下文缓存的 token 数。

total_tokens
integer
required
该请求中，所有 token 的数量（prompt + completion）。

completion_tokens_details

object