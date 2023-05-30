package com.example.bilingualb8.repositories.custom.impl;

import com.example.bilingualb8.dto.responses.answer.UserAnswerResponse;
import com.example.bilingualb8.dto.responses.questions.ResultQuestionResponse;
import com.example.bilingualb8.dto.responses.result.EvaluatingSubmittedResultResponse;
import com.example.bilingualb8.dto.responses.result.SubmittedResultsResponse;
import com.example.bilingualb8.dto.responses.userResult.MyResultResponse;
import com.example.bilingualb8.enums.AnswerStatus;
import com.example.bilingualb8.enums.QuestionType;
import com.example.bilingualb8.enums.ResultStatus;
import com.example.bilingualb8.exceptions.NotFoundException;
import com.example.bilingualb8.repositories.custom.CustomAnswerRepository;
import com.example.bilingualb8.repositories.custom.CustomResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Repository
@RequiredArgsConstructor
public class CustomResultRepositoryImpl implements CustomResultRepository {
    private final JdbcTemplate jdbcTemplate;
    private final CustomAnswerRepository customAnswerRepository;

    @Override
    public List<MyResultResponse> getAll(Long userId) {

        String sql = """
                SELECT r.id as id,
                       r.date_of_submission as date_of_submission,
                       t.title as test_title,
                       r.status as status,
                       r.score as score
                FROM results r
                JOIN users u ON r.user_id = u.id
                JOIN tests t ON r.test_id = t.id
                WHERE u.id = ?;
                """;

        return jdbcTemplate.query(sql, (resultSet, i) ->
                        new MyResultResponse(
                                resultSet.getLong("id"),
                                resultSet.getObject("date_of_submission", LocalDate.class),
                                resultSet.getString("test_title"),
                                ResultStatus.valueOf(resultSet.getString("status")),
                                resultSet.getInt("score")
                        ),
                userId
        );
    }

    @Override
    public List<SubmittedResultsResponse> getAllSubmittedResults() {

        String sql = """
                SELECT r.id as id,
                CONCAT(u.first_name, ' ', u.last_name) as user_full_name,
                r.date_of_submission as date_of_submission,
                t.title as title,
                r.status as status,
                r.score as score
                FROM results r
                JOIN users u ON r.user_id = u.id
                JOIN tests t on r.test_id = t.id
                """;

        return jdbcTemplate.query(sql, (resultSet, i) ->
                new SubmittedResultsResponse(
                        resultSet.getLong("id"),
                        resultSet.getString("user_full_name"),
                        resultSet.getObject("date_of_submission", LocalDateTime.class),
                        resultSet.getString("title"),
                        ResultStatus.valueOf(resultSet.getString("status")),
                        resultSet.getFloat("score")
                )
        );
    }

    @Override
    public EvaluatingSubmittedResultResponse getByIdEvaluatingSubmittedResult(Long resultId) {
        EvaluatingSubmittedResultResponse evaluatingSubmittedResultResponse = getEvaluatingSubmittedResultResponses(resultId);
        List<ResultQuestionResponse> resultQuestionResponses = getResultQuestionResponses(resultId);
        List<UserAnswerResponse> evaluatingAnswerRespons = customAnswerRepository.getAnswerResponsesByResultId(resultId);

        for (ResultQuestionResponse resultQuestionResponse : resultQuestionResponses) {
            List<UserAnswerResponse> answers = evaluatingAnswerRespons.stream()
                    .filter(answer -> Objects.equals(answer.getQuestionId(), resultQuestionResponse.getQuestionId()))
                    .toList();
            resultQuestionResponse.setUserAnswerResponse(answers);

            // Check if all AnswerResponse objects have the status EVALUATED
            boolean allEvaluated = answers.stream()
                    .allMatch(answer -> answer.getAnswerStatus() == AnswerStatus.EVALUATED);

            if (allEvaluated) {
                resultQuestionResponse.setAnswerStatus(AnswerStatus.EVALUATED);
            }else {
                resultQuestionResponse.setAnswerStatus(AnswerStatus.NOT_EVALUATED);
            }
        }

        evaluatingSubmittedResultResponse.setResultQuestionResponses(resultQuestionResponses);

        return evaluatingSubmittedResultResponse;
    }

    @Override
    public Long getResultIdByAnswerId(Long answerId) {

        String sql = """
            SELECT r.id as id
            FROM results r
            JOIN results_answers ra on r.id = ra.result_id
            JOIN answers a on ra.answers_id = a.id
            WHERE a.id = ?
            """;

        return jdbcTemplate.queryForObject(sql, new Object[]{answerId}, (resultSet, i) ->
                resultSet.getLong("id"));
    }


    private List<ResultQuestionResponse> getResultQuestionResponses(Long resultId) {
        String questionQuery = """
            SELECT q.id as id,
            q.question_type as question_type,
            a.evaluated_score as score
            FROM tests t
            JOIN questions q on t.id = q.test_id
            JOIN results r on t.id = r.test_id
            JOIN answers a on q.id = a.question_id
            WHERE r.id = ?
            """;

        return jdbcTemplate.query(questionQuery, (resultSet, i) ->
                        new ResultQuestionResponse(
                                resultSet.getLong("id"),
                                QuestionType.valueOf(resultSet.getString("question_type")),
                                resultSet.getFloat("score") // when I added score was some warn todo
                        ),
                resultId
        );
    }

    private EvaluatingSubmittedResultResponse getEvaluatingSubmittedResultResponses(Long resultId) {
        String resultQuery = """
            SELECT r.id as id,
            concat(u.first_name, ' ', u.last_name) as full_name,
            r.date_of_submission as date_of_submission,
            t.title as title,
            r.score as score,
            r.status as status
            FROM results r
            JOIN users u on u.id = r.user_id
            JOIN tests t on r.test_id = t.id
            WHERE r.id = ?
            """;

        return jdbcTemplate.query(resultQuery, (resultSet, i) ->
                        new EvaluatingSubmittedResultResponse(
                                resultSet.getLong("id"),
                                resultSet.getString("full_name"),
                                resultSet.getString("title"),
                                resultSet.getObject("date_of_submission", LocalDateTime.class),
                                resultSet.getFloat("score"),
                                ResultStatus.valueOf(resultSet.getString("status"))
                        ),
                resultId
        ).stream().findFirst().orElseThrow(()-> new NotFoundException("first index was empty"));
    }
}
