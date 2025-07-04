package com.mygym.crm.backstages.core.services;

import com.mygym.crm.backstages.core.dtos.request.trainingdto.TrainingDto;
import com.mygym.crm.backstages.core.services.communication.TrainerContributionQueueSender;
import com.mygym.crm.backstages.core.services.mapper.TrainerMapper;
import com.mygym.crm.backstages.domain.models.Trainee;
import com.mygym.crm.backstages.domain.models.Trainer;
import com.mygym.crm.backstages.domain.models.Training;
import com.mygym.crm.backstages.domain.models.TrainingType;
import com.mygym.crm.backstages.exceptions.custom.NoTrainerException;
import com.mygym.crm.backstages.interfaces.daorepositories.TraineeDao;
import com.mygym.crm.backstages.interfaces.daorepositories.TrainerDao;
import com.mygym.crm.backstages.interfaces.daorepositories.TrainingDao;
import com.mygym.crm.backstages.interfaces.services.TrainingService;
import com.mygym.crm.sharedmodule.TrainerWorkloadDto;
import org.hibernate.HibernateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;

import java.util.Optional;
import java.util.UUID;

import static com.mygym.crm.sharedmodule.ActionEnum.ADD;

@Service
public class TrainingServiceImpl implements TrainingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TrainingServiceImpl.class);
    private final TrainingDao trainingDao;
    private final TrainerDao trainerDao;
    private final TraineeDao traineeDao;
    private final TrainerMapper trainerMapper;
    private final TrainerContributionQueueSender trainerContributionQueueSender;

    @Autowired
    public TrainingServiceImpl(TrainingDao trainingDao, TrainerDao trainerDao, TraineeDao traineeDao, TrainerMapper trainerMapper,
                               TrainerContributionQueueSender trainerContributionQueueSender) {
        this.trainingDao = trainingDao;
        this.trainerDao = trainerDao;
        this.traineeDao = traineeDao;
        this.trainerMapper = trainerMapper;
        this.trainerContributionQueueSender = trainerContributionQueueSender;
    }

    @Transactional
    @Override
    public Optional<Training> add(TrainingDto trainingDto, Model model) {
        String transactionId = UUID.randomUUID().toString();
        MDC.put("transactionId", transactionId);

        try {
            Training newTraining = new Training();
            LOGGER.info("New Training, populating it with given traineeDTO");

            newTraining.setTrainingName(trainingDto.getTrainingName());
            newTraining.setTrainingDate(trainingDto.getTrainingDate());
            newTraining.setTrainingDuration(trainingDto.getTrainingDuration());

            Trainer trainer = trainerDao.select(trainingDto.getTrainerId()).orElseThrow(() -> {
                LOGGER.error("Trainer with id {} not found", trainingDto.getTrainerId());
                return new NoTrainerException("Could not found the Trainer");
            });
            Trainee trainee = traineeDao.select(trainingDto.getTraineeId()).orElseThrow(() -> {
                LOGGER.error("Trainee with id {} not found", trainingDto.getTraineeId());
                return new NoTrainerException("Could not found the Trainee");
            });

            TrainingType trainingType = trainer.getTrainingType();

            newTraining.setTrainer(trainer);
            newTraining.setTrainee(trainee);
            newTraining.setTrainingType(trainingType);

            LOGGER.info("Trying to new create training");
            Optional<Training> optionalTraining = trainingDao.add(newTraining);

            optionalTraining.ifPresentOrElse(
                    (training) -> {
                        TrainerWorkloadDto trainingWorkloadDto = trainerMapper.mapTrainingToTrainerWorkloadDto(training);
                        trainingWorkloadDto.setActionType(ADD);
                        trainerContributionQueueSender.sendMessage(trainingWorkloadDto);
                        LOGGER.info("Training with trainingId: {} has been created", training.getId());
                    },
                    () -> LOGGER.warn("Training could not be created")
            );

            return optionalTraining;
        } finally {
            MDC.remove("transactionId");
        }
    }

    @Transactional
    @Override
    public int deleteWithTraineeUsername(String traineeUsername) {
        String transactionId = UUID.randomUUID().toString();
        MDC.put("transactionId", transactionId);

        try {
            int deletedRows = trainingDao.deleteWithTraineeUsername(traineeUsername);

            if (deletedRows > 0) {
                LOGGER.info("deleted {} number of rows for Training with trainee userName: {}", deletedRows, traineeUsername);
            } else {
                LOGGER.warn("Could not delete the rows for Training for given trainee userName: {}", traineeUsername);
            }

            return deletedRows;
        } finally {
            MDC.remove("transactionId");
        }
    }

    @Transactional(noRollbackFor = HibernateException.class, readOnly = true)
    @Override
    public Optional<Training> getById(Long id) {
        String transactionId = UUID.randomUUID().toString();
        MDC.put("transactionId", transactionId);

        try {
            LOGGER.info("Trying to find Training with ID: {}", id);

            Optional<Training> trainingOptional = trainingDao.select(id);

            trainingOptional.ifPresentOrElse(
                    training -> {
                        training.getTrainee().getUserId();
                        training.getTrainer().getUserId();
                        training.getTrainingType().getTrainingTypeId();
                        LOGGER.info("Found Training with ID: {}", id);
                    },
                    () -> LOGGER.warn("No training found with ID: {}", id)
            );

            return trainingOptional;
        } finally {
            MDC.remove("transactionId");
        }
    }


}
