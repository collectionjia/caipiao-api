package com.xytl.project.caipiaoapi.service.questions;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xytl.project.caipiaoapi.domain.questions.Questions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Questions
 */
@Service
@ConditionalOnProperty(name = "mysql.enabled", havingValue = "true", matchIfMissing = true)
public class QuestionsService {

	@Autowired
    private QuestionsMapper questionsMapper;

    public Questions get(Integer id) {
        return questionsMapper.selectById(id);
    }

    public List<Questions> listAll() {
        return questionsMapper.selectList(Wrappers.emptyWrapper());
    }

    public Questions create(Questions questions) {
        questionsMapper.insert(questions);
        return questions;
    }

    public int update(Questions questions) {
        return questionsMapper.updateById(questions);
    }

    public int remove(Integer id) {
        return questionsMapper.deleteById(id);
    }



}
