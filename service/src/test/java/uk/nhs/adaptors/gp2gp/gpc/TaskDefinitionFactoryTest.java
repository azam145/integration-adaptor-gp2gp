package uk.nhs.adaptors.gp2gp.gpc;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.when;

import static uk.nhs.adaptors.gp2gp.common.task.TaskType.GET_GPC_DOCUMENT;
import static uk.nhs.adaptors.gp2gp.common.task.TaskType.GET_GPC_STRUCTURED;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import uk.nhs.adaptors.gp2gp.common.task.TaskDefinitionFactory;
import uk.nhs.adaptors.gp2gp.common.task.TaskHandlerException;

@ExtendWith(MockitoExtension.class)
public class TaskDefinitionFactoryTest {
    private static final String DOCUMENT_ID_VALUE = "document_id_example";
    private static final String NHS_NUMBER_VALUE = "nhs_number_example";
    private static final String CONVERSATION_ID_VALUE = "conversation_id_example";
    private static final String REQUEST_ID_VALUE = "request_id_example";
    private static final String TASK_ID_VALUE = "task_id_example";
    private static final String TASK_ID_KEY = "taskId";
    private static final String DOCUMENT_ID_KEY = "documentId";
    private static final String REQUEST_ID_KEY = "requestId";
    private static final String CONVERSATION_ID_KEY = "conversationId";
    private static final String NHS_NUMBER_KEY = "nhsNumber";
    private static final String DOCUMENT_BODY = addCustomValueToJsonString(DOCUMENT_ID_KEY, DOCUMENT_ID_VALUE);
    private static final String STRUCTURE_BODY = addCustomValueToJsonString(NHS_NUMBER_KEY, NHS_NUMBER_VALUE);
    private static final String UNKNOWN_TASK_TYPE = "unknownTaskType";

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private TaskDefinitionFactory taskDefinitionFactory;

    private static String addCustomValueToJsonString(String customKey, String customValue) {
        try {
            return new JSONObject()
                .put(TASK_ID_KEY, TASK_ID_VALUE)
                .put(REQUEST_ID_KEY, REQUEST_ID_VALUE)
                .put(CONVERSATION_ID_KEY, CONVERSATION_ID_VALUE)
                .put(customKey, customValue)
                .toString();
        } catch (JSONException e) {
            throw new IllegalStateException("Error constructing buildJsonStringForStructure");
        }
    }

    @Test
    public void When_GettingValidDocumentTaskDefinition_Expect_TaskDefinitionFactoryReturnsDocumentTaskDefinition()
        throws JsonProcessingException, TaskHandlerException {
        when(objectMapper.readValue(DOCUMENT_BODY, GetGpcDocumentTaskDefinition.class)).thenReturn(
            GetGpcDocumentTaskDefinition.builder()
                .taskId(TASK_ID_VALUE)
                .conversationId(CONVERSATION_ID_VALUE)
                .documentId(DOCUMENT_ID_VALUE)
                .build()
        );

        GetGpcDocumentTaskDefinition taskDefinition =
            (GetGpcDocumentTaskDefinition) taskDefinitionFactory
                .getTaskDefinition(GET_GPC_DOCUMENT.getTaskName(), DOCUMENT_BODY);

        assertThat(taskDefinition.getClass()).isEqualTo(GetGpcDocumentTaskDefinition.class);
    }

    @Test
    public void When_GettingValidStructureTaskDefinition_Expect_TaskDefinitionFactoryReturnsStructureTaskDefinition()
        throws JsonProcessingException, TaskHandlerException {
        when(objectMapper.readValue(STRUCTURE_BODY, GetGpcStructuredTaskDefinition.class)).thenReturn(
            GetGpcStructuredTaskDefinition.builder()
                .taskId(TASK_ID_VALUE)
                .conversationId(CONVERSATION_ID_VALUE)
                .nhsNumber(NHS_NUMBER_VALUE)
                .requestId(REQUEST_ID_VALUE)
                .build()
        );

        GetGpcStructuredTaskDefinition taskDefinition =
            (GetGpcStructuredTaskDefinition) taskDefinitionFactory
                .getTaskDefinition(GET_GPC_STRUCTURED.getTaskName(), STRUCTURE_BODY);

        assertThat(taskDefinition.getClass()).isEqualTo(GetGpcStructuredTaskDefinition.class);
    }

    @Test
    public void When_GettingInvalidTaskDefinition_Expect_TaskDefinitionFactoryReturnsOptionalEmpty() {
        assertThatThrownBy(() -> taskDefinitionFactory.getTaskDefinition(UNKNOWN_TASK_TYPE, "body"))
            .isInstanceOf(TaskHandlerException.class)
            .hasMessageContaining(UNKNOWN_TASK_TYPE);
    }
}
