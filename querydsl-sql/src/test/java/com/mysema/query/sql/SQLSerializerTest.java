/*
 * Copyright (c) 2010 Mysema Ltd.
 * All rights reserved.
 *
 */
package com.mysema.query.sql;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.mysema.query.BooleanBuilder;
import com.mysema.query.SelectBaseTest.Survey;
import com.mysema.query.sql.domain.QSurvey;
import com.mysema.query.support.Expressions;
import com.mysema.query.types.Expression;
import com.mysema.query.types.SubQueryExpression;
import com.mysema.query.types.path.PathBuilder;

public class SQLSerializerTest {

    @Test
    public void StartsWith(){
        SQLSerializer serializer = new SQLSerializer(SQLTemplates.DEFAULT);
        QSurvey s1 = new QSurvey("s1");
        serializer.handle(s1.name.startsWith("X"));
        assertEquals("s1.NAME like ? escape '\\'", serializer.toString());
        assertEquals(Arrays.asList("X%"), serializer.getConstants());
    }
    
    @Test
    public void From_Function() {
        SQLQuery query = query();
        QSurvey survey = QSurvey.survey;
        query.from(Expressions.template(Survey.class, "functionCall()")).join(survey);
        query.where(survey.name.isNotNull());
        assertEquals("from functionCall()\njoin SURVEY SURVEY\nwhere SURVEY.NAME is not null", query.toString());
    }
    
    @Test
    public void Join_To_Function_With_Alias() {
        SQLQuery query = query();
        QSurvey survey = QSurvey.survey;
        query.from(survey).join(RelationalFunctionCall.create(Survey.class, "functionCall"), Expressions.path(Survey.class, "fc"));
        query.where(survey.name.isNotNull());
        assertEquals("from SURVEY SURVEY\njoin functionCall() as fc\nwhere SURVEY.NAME is not null", query.toString());
    }
    
    @Test
    public void Complex_SubQuery() {
        QSurvey survey = QSurvey.survey;
        
        // create sub queries
        List<SubQueryExpression<Object[]>> sq = new ArrayList<SubQueryExpression<Object[]>>();
        String[] strs = new String[]{"a","b","c"};
        for(String str : strs) {
            Expression<Boolean> alias = Expressions.cases().when(survey.name.eq(str)).then(true).otherwise(false);
            sq.add(sq().from(survey).distinct().unique(survey.name, alias));             
        }
        
        // master query
        PathBuilder<Object[]> subAlias = new PathBuilder<Object[]>(Object[].class, "sub");        
        SubQueryExpression<?> master = sq()
                .from(sq().union(sq).as(subAlias))
                .groupBy(subAlias.get("prop1"))
                .list(subAlias.get("prop2"));
        
        SQLSerializer serializer = new SQLSerializer(SQLTemplates.DEFAULT);
        serializer.serialize(master.getMetadata(), false);
        System.err.println(serializer);
    }
    
    private SQLQuery query() {
        return new SQLQueryImpl(SQLTemplates.DEFAULT);
    }
    
    private SQLSubQuery sq() {
        return new SQLSubQuery();
    }
        
    @Test
    public void Boolean(){
        QSurvey s = new QSurvey("s");
        BooleanBuilder bb1 = new BooleanBuilder();
        bb1.and(s.name.eq(s.name));

        BooleanBuilder bb2 = new BooleanBuilder();
        bb2.or(s.name.eq(s.name));
        bb2.or(s.name.eq(s.name));

        String str = new SQLSerializer(SQLTemplates.DEFAULT).handle(bb1.and(bb2)).toString();
        assertEquals("s.NAME = s.NAME and (s.NAME = s.NAME or s.NAME = s.NAME)", str);
    }
    


}
