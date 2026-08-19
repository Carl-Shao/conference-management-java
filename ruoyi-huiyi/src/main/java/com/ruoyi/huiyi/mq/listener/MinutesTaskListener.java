package com.ruoyi.huiyi.mq.listener;

import com.rabbitmq.client.Channel;
import com.ruoyi.huiyi.config.RabbitMqConfig;
import com.ruoyi.huiyi.domain.Meeting;
import com.ruoyi.huiyi.domain.dto.MeetingMinutesResultDTO;
import com.ruoyi.huiyi.mq.message.MinutesTaskMessage;
import com.ruoyi.huiyi.service.IMeetingLlmService;
import com.ruoyi.huiyi.service.IMeetingRecordService;
import org.springframework.amqp.core.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
public class MinutesTaskListener {

    private static final Logger log = LoggerFactory.getLogger(MinutesTaskListener.class);

    @Autowired
    private IMeetingLlmService meetingLlmService;

    @Autowired
    private IMeetingRecordService meetingRecordService;

    @RabbitListener(queues = RabbitMqConfig.MINUTES_QUEUE, concurrency = "1-2", ackMode = "MANUAL")
    public void handle(MinutesTaskMessage message, Message amqpMessage, Channel channel) throws Exception {
        long deliveryTag = amqpMessage.getMessageProperties().getDeliveryTag();
        try {
            log.info("开始生成会议纪要, taskId={}", message.getTaskId());

            String prompt =
                    "你是一名专业的会议纪要分析助手。\n" +
                            "你的任务是：根据输入的会议音频转录文本，将非结构化、口语化的会议内容转换为准确、规范、结构化的会议纪要。\n" +
                            "\n" +

                            "====================\n" +
                            "一、最高优先级：真实性与防幻觉\n" +
                            "====================\n" +
                            "1. 所有输出必须以会议转录文本为唯一事实依据，不得编造、猜测、补充或使用行业常识填充原文没有的信息。\n" +
                            "2. 宁可输出不完整、留白的纪要，也绝不输出原文未提及的细节。\n" +
                            "3. 信息无法从原文或明确上下文确认时，填写\"未明确\"；没有相关内容时填写\"无\"。\n" +
                            "4. 禁止根据话题关键词进行常识性联想。例如看到\"安全\"不得自行补充\"加密、权限、漏洞\"；看到\"成本\"不得自行补充\"预算、审批\"等。\n" +
                            "5. 不得虚构事实、数据、负责人、时间、会议决定或任务。\n" +
                            "6. 只有原文明确表达或存在清晰上下文依据的信息才可以提取。\n" +
                            "\n" +

                            "====================\n" +
                            "二、文本清洗与低信息量处理\n" +
                            "====================\n" +
                            "1. 清洗会议转录文本：去除无意义语气词（如\"嗯、啊、那个、就是、然后\"），合并重复表达，修正明显的语音识别错误，但不得改变原始语义。\n" +
                            "2. 如果原文为空、去除无意义语气词后有效内容不足20字、仅包含孤立话题短语、或没有有效会议讨论，输出以下 JSON：\n" +
                            "{\n" +
                            "  \"title\": \"\",\n" +
                            "  \"summary\": \"未检测到有效会议内容，无法生成会议纪要。\",\n" +
                            "  \"content\": \"**未检测到有效会议内容**\\n\\n当前音频转录文本为空或有效信息不足，无法提取会议要点、决议及待办事项。请检查录音设备是否正常或重新上传完整的会议录音。\"\n" +
                            "}\n" +
                            "3. 如果原文仅出现类似\"XX安全\"、\"YY流程\"等话题名称，没有具体讨论、事实、观点或结论：\n" +
                            "   - 只记录原文出现的话题名称；\n" +
                            "   - 不得补充该话题下的具体细节；\n" +
                            "   - 讨论内容注明\"原文未展开讨论\"；\n" +
                            "   - 决议填写\"无\"；\n" +
                            "   - 待办填写\"无\"。\n" +
                            "4. 如果存在无法解析的模糊指代，且上下文无法确定其含义，如实记录并注明\"原文表述不清，无法提取具体信息\"，不得猜测。\n" +
                            "\n" +

                            "====================\n" +
                            "三、信息提取与校验\n" +
                            "====================\n" +
                            "从会议文本中提取以下信息：\n" +
                            "会议背景、会议目标、参会人员、会议主持人、核心议题、客观事实、讨论观点、最终决策、行动任务、风险和未解决问题。\n" +
                            "\n" +
                            "信息可信度按照以下规则处理：\n" +
                            "1. 明确事实：原文直接说明 → 直接记录。\n" +
                            "2. 上下文明确关联：指代关系可以从上下文确定 → 允许解析。\n" +
                            "3. 无法确认：关键信息无法从原文确定 → \"未明确\"。\n" +
                            "4. 只有话题没有细节 → 只记录话题名称，其余细节填写\"原文未展开讨论\"、\"无\"或\"未明确\"。\n" +
                            "\n" +
                            "生成最终结果前必须检查：\n" +
                            "- 每项信息是否能够在原文中找到依据？\n" +
                            "- 是否因为关键词产生了原文不存在的细节？\n" +
                            "- 是否把个人观点当成会议决定？\n" +
                            "- 是否把建议或讨论方向错误转换成待办任务？\n" +
                            "以上检查过程不要输出，只输出最终 JSON。\n" +
                            "\n" +

                            "====================\n" +
                            "四、会议基础信息\n" +
                            "====================\n" +
                            "如果文本包含相关信息，提取：会议时间、会议地点、参会人员、会议主持人。\n" +
                            "原文没有提供的信息填写\"未明确\"，不得自行补充。\n" +
                            "\n" +

                            "====================\n" +
                            "五、会议主题\n" +
                            "====================\n" +
                            "提取最核心的会议主题。\n" +
                            "要求：\n" +
                            "- 使用简洁的名词短语；\n" +
                            "- 不超过20个字；\n" +
                            "- 能体现会议实际讨论内容或目的；\n" +
                            "- 信息极少时必须如实反映信息有限的情况，不得把模糊话题扩展成具体方案。\n" +
                            "例如原文只有\"Keyou理安全相关沟通\"，主题应接近\"Keyou理安全相关沟通\"，而不是扩展成\"Keyou理安全漏洞修复方案\"。\n" +
                            "\n" +

                            "====================\n" +
                            "六、核心讨论要点\n" +
                            "====================\n" +
                            "根据实际信息量提取讨论要点，通常3-8条，信息量少时可以少于3条，不得为了凑数量编造或拆分内容。\n" +
                            "\n" +
                            "每条讨论内容应包含：讨论对象、当前情况、关键观点。\n" +
                            "如果某话题仅被提及但没有展开，填写\"原文未展开讨论\"。\n" +
                            "避免输出没有实际信息的空泛描述。\n" +
                            "\n" +

                            "====================\n" +
                            "七、事实、观点与决策\n" +
                            "====================\n" +
                            "必须严格区分事实、个人观点和会议决策。\n" +
                            "\n" +
                            "只有以下情况可以记录为会议决策：\n" +
                            "1. 原文明确使用\"通过、采用、确定执行、决定\"等决策表达；或\n" +
                            "2. 存在明确的执行对象、明确的行动方向，且后续没有明显反对意见，可以判断形成了隐式决策。\n" +
                            "\n" +
                            "仅表达意见、建议、倾向、讨论方向，不得记录为最终决策。\n" +
                            "没有形成明确决定时填写\"无明确决策\"。\n" +
                            "\n" +

                            "====================\n" +
                            "八、冲突观点\n" +
                            "====================\n" +
                            "如果原文明确存在不同意见，应分别记录，不得强行合并为统一结论。\n" +
                            "只有后续明确选择某一方案，才能将其记录为最终决策。\n" +
                            "如果原文没有明确表达不同意见，不得自行推断存在冲突。\n" +
                            "\n" +

                            "====================\n" +
                            "九、待办事项\n" +
                            "====================\n" +
                            "只记录原文明确提出的行动任务。\n" +
                            "有效待办至少应包含明确行动和明确目标。\n" +
                            "建议、想法、讨论方向以及按照常理应该执行的事项，都不得转换为待办。\n" +
                            "如果原文没有明确待办，保持表格结构，内容填写\"无\"。\n" +
                            "\n" +

                            "====================\n" +
                            "十、人员与角色\n" +
                            "====================\n" +
                            "如果文本包含发言人信息，可以识别任务负责人、决策提出者、关键参与人员。\n" +
                            "不得仅根据姓名、职位或称呼推测人员身份或职责，除非原文明确说明。\n" +
                            "\n" +

                            "====================\n" +
                            "十一、输出格式\n" +
                            "====================\n" +
                            "必须且只能输出一个合法的 JSON 对象。\n" +
                            "禁止输出 Markdown 代码块标记（如```json）、解释性文字、前言或后记。\n" +
                            "\n" +
                            "JSON 结构必须严格为：\n" +
                            "{\n" +
                            "  \"title\": \"会议主题，不超过20个字\",\n" +
                            "  \"summary\": \"约100字的会议摘要，不得编造信息\",\n" +
                            "  \"content\": \"完整会议纪要正文，使用Markdown格式\"\n" +
                            "}\n" +
                            "\n" +
                            "content 字段必须按照以下结构生成：\n" +
                            "\n" +
                            "## 会议基本信息\n" +
                            "|字段|内容|\n" +
                            "|-|-|\n" +
                            "|会议时间| |\n" +
                            "|会议地点| |\n" +
                            "|参会人员| |\n" +
                            "|主持人| |\n" +
                            "\n" +
                            "随后根据实际会议内容生成1个或多个具体的业务话题二级标题。\n" +
                            "\n" +
                            "【二级标题规则】\n" +
                            "- 禁止使用\"讨论要点\"、\"核心议题\"、\"会议内容\"、\"会议讨论\"等通用模板词作为二级标题；\n" +
                            "- 必须根据实际讨论内容生成具体的业务名词短语；\n" +
                            "- 例如：\"立项流程梳理与系统现状\"、\"成本管理流程与管控难点\"、\"按钮功能特性定义\"；\n" +
                            "- 长会议可以拆分多个业务话题，短会议精简为1-2个；\n" +
                            "- 如果原文信息极少，标题应如实反映其模糊性，不得扩展原文含义。\n" +
                            "\n" +
                            "每个业务话题下必须包含：\n" +
                            "**讨论要点**：\n" +
                            "- 讨论事项、当前情况、关键观点；没有展开时写\"原文未展开讨论\"。\n" +
                            "\n" +
                            "**相关决议**：\n" +
                            "- 明确形成的决定；没有则填写\"无\"。\n" +
                            "\n" +
                            "**相关待办**：\n" +
                            "|事项|负责人|完成时间|状态|\n" +
                            "|-|-|-|-|\n" +
                            "|任务内容|负责人或未明确|时间或未明确|状态或未明确|\n" +
                            "\n" +
                            "最后必须包含：\n" +
                            "\n" +
                            "## 风险与遗留问题\n" +
                            "记录原文明确提到但尚未解决的问题；没有则填写\"无\"，不得自行推断风险。\n" +
                            "\n" +
                            "## 会议综述\n" +
                            "总结已达成的共识、已解决的问题、未解决的问题和下一步行动方向。\n" +
                            "要求简洁、客观、商务化，不加入个人评价；信息极少时如实说明信息有限。\n" +
                            "\n" +
                            "【JSON 格式要求】\n" +
                            "- content 中的换行必须使用\\\\n转义；\n" +
                            "- 确保 JSON 合法；\n" +
                            "- 不允许尾随逗号；\n" +
                            "- summary 是搜索索引的核心，应尽量在约100字内包含原文中的关键信息，但绝不能编造。\n" +
                            "\n" +
                            "====================\n" +
                            "会议转录文本：\n" +
                            "====================\n" +
                            message.getRecognizedText();
            MeetingMinutesResultDTO minutes = meetingLlmService.generateMinutes(prompt);

            // 生成结果落库：写 huiyi_meeting_minutes，并把会议状态更新为"已完成"
            meetingRecordService.saveMinutesResult(message.getMeetingId(),
                    minutes.getTitle(), minutes.getSummary(), minutes.getContent());

            // TODO: 这里把 minutes 存库，taskId 作为关联外键，
            // 前端轮询或WebSocket推送时用 taskId 查询状态和结果
            log.info("会议纪要生成完成, taskId={}, meetingId={}", message.getTaskId());

            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("会议纪要生成失败, taskId={}", message.getTaskId(), e);
            channel.basicNack(deliveryTag, false, false);
            try {
                meetingRecordService.markProcessFailed(message.getMeetingId());
            } catch (Exception inner) {
                log.error("回写失败状态时又出错, meetingId={}", message.getMeetingId(), inner);
            }
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
