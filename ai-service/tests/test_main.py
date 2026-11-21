from app.main import MatchRequest, calculate_match


def test_weighted_matching_is_deterministic():
    result = calculate_match(MatchRequest(required_skills=["Java", "Spring Boot"], candidate_skills=["java"], candidate_years=1, job_experience_min=2, framework_score=80, project_score=70, title_score=90, location_score=100))
    assert result.overall_score == 63.0
    assert result.missing_skills == ["Spring Boot"]
    assert result.experience_compatible is False
