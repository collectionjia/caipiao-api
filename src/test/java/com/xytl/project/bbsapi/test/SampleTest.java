package com.xytl.project.bbsapi.test;

import java.util.List;

import javax.annotation.Resource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.xytl.project.caipiaoapi.domain.piaocoder.PiaoCoder;
import com.xytl.project.caipiaoapi.service.piaocoder.PiaoCoderMapper;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class SampleTest {

    @Resource
    private PiaoCoderMapper piaoCoderMapper;

    @Test
    public void testSelect() {
        System.out.println(("----- selectAll method test ------"));
        List<PiaoCoder> userList = piaoCoderMapper.selectList(null);
        userList.forEach(System.out::println);
    }
}
