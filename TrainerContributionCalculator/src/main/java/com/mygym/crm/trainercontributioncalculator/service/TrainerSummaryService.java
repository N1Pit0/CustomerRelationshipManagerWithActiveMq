package com.mygym.crm.trainercontributioncalculator.service;

import com.mygym.crm.sharedmodule.TrainerWorkloadDto;
import com.mygym.crm.trainercontributioncalculator.domain.models.TrainerSummary;

public interface TrainerSummaryService {
    TrainerSummary createTrainerSummary(TrainerWorkloadDto trainerWorkloadDto);

    TrainerSummary updateTrainerSummary(TrainerSummary trainerSummary);

    TrainerSummary findByUsername(String username);

}
