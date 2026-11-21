"""Private AI-analysis service. It never sends mail or mutates application data."""
from fastapi import FastAPI
from pydantic import BaseModel, Field, field_validator

app = FastAPI(title="ApplyPilot AI Service", version="0.1.0")


class MatchRequest(BaseModel):
    required_skills: list[str] = Field(default_factory=list)
    candidate_skills: list[str] = Field(default_factory=list)
    candidate_years: float = Field(ge=0, le=50)
    job_experience_min: float | None = Field(default=None, ge=0, le=50)
    job_experience_max: float | None = Field(default=None, ge=0, le=50)
    framework_score: float = Field(default=0, ge=0, le=100)
    project_score: float = Field(default=0, ge=0, le=100)
    title_score: float = Field(default=0, ge=0, le=100)
    location_score: float = Field(default=0, ge=0, le=100)

    @field_validator("required_skills", "candidate_skills")
    @classmethod
    def non_blank_skills(cls, values: list[str]) -> list[str]:
        return [item.strip() for item in values if item.strip()]


class MatchResponse(BaseModel):
    overall_score: float
    required_skills_score: float
    experience_score: float
    framework_score: float
    project_score: float
    title_score: float
    location_score: float
    matched_skills: list[str]
    missing_skills: list[str]
    experience_compatible: bool
    explanation: str


def normalize(skill: str) -> str:
    return " ".join(skill.lower().split())


def calculate_match(request: MatchRequest) -> MatchResponse:
    required = {normalize(s): s for s in request.required_skills}
    candidate = {normalize(s) for s in request.candidate_skills}
    matched = [original for key, original in required.items() if key in candidate]
    missing = [original for key, original in required.items() if key not in candidate]
    skills_score = 100.0 if not required else round(100 * len(matched) / len(required), 2)
    if request.job_experience_min is None:
        experience_compatible, experience_score = True, 100.0
    elif request.candidate_years >= request.job_experience_min:
        experience_compatible, experience_score = True, 100.0
    else:
        experience_compatible = False
        experience_score = round(100 * request.candidate_years / request.job_experience_min, 2)
    total = round(skills_score * .40 + experience_score * .20 + request.framework_score * .15 + request.project_score * .10 + request.title_score * .10 + request.location_score * .05, 2)
    explanation = f"Matched {len(matched)} of {len(required)} required skills; experience is {'compatible' if experience_compatible else 'below the stated minimum'}."
    return MatchResponse(overall_score=total, required_skills_score=skills_score, experience_score=experience_score, framework_score=request.framework_score, project_score=request.project_score, title_score=request.title_score, location_score=request.location_score, matched_skills=matched, missing_skills=missing, experience_compatible=experience_compatible, explanation=explanation)


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "UP", "service": "applypilot-ai"}


@app.post("/ai/v1/match", response_model=MatchResponse)
def match(request: MatchRequest) -> MatchResponse:
    return calculate_match(request)
